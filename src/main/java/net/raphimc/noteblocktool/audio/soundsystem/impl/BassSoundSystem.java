/*
 * This file is part of NoteBlockTool - https://github.com/RaphiMC/NoteBlockTool
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.noteblocktool.audio.soundsystem.impl;

import com.sun.jna.Memory;
import com.sun.jna.ptr.FloatByReference;
import net.raphimc.audiomixer.soundmodifier.impl.NormalizationModifier;
import net.raphimc.audiomixer.util.GrowableArray;
import net.raphimc.noteblocktool.audio.soundsystem.BassLibrary;
import net.raphimc.noteblocktool.audio.soundsystem.BassMixLibrary;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SoundFileUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassSoundSystem extends SoundSystem {

    private static BassSoundSystem instance;

    public static BassSoundSystem createPlayback(final Map<String, byte[]> soundData, final int maxSounds) {
        if (instance != null) {
            throw new IllegalStateException("BASS sound system already initialized");
        }
        if (!BassLibrary.isLoaded()) {
            throw new IllegalStateException("BASS library is not available");
        }
        instance = new BassSoundSystem(soundData, maxSounds);
        return instance;
    }

    public static BassSoundSystem createCapture(final Map<String, byte[]> soundData, final int maxSounds, final AudioFormat captureAudioFormat) {
        if (instance != null) {
            throw new IllegalStateException("BASS sound system already initialized");
        }
        if (!BassLibrary.isLoaded()) {
            throw new IllegalStateException("BASS library is not available");
        }
        instance = new BassSoundSystem(soundData, maxSounds, captureAudioFormat);
        return instance;
    }


    private final Map<String, Integer> soundSamples = new HashMap<>();
    private final List<Integer> playingChannels = new ArrayList<>();
    private final AudioFormat captureAudioFormat;
    private Thread shutdownHook;
    private int captureChannel;
    private Memory captureMemory;
    private NormalizationModifier captureNormalizer;

    @SuppressWarnings("FieldCanBeLocal")
    private final BassLibrary.SYNCPROC channelFreeSync = (handle, channel, data, user) -> {
        synchronized (this) {
            this.playingChannels.remove((Integer) channel);
        }
    };

    private BassSoundSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        this(soundData, maxSounds, null);
    }

    private BassSoundSystem(final Map<String, byte[]> soundData, final int maxSounds, final AudioFormat captureAudioFormat) {
        super(maxSounds);

        this.captureAudioFormat = captureAudioFormat;
        final int version = BassLibrary.INSTANCE.BASS_GetVersion();
        if (((version >> 16) & 0xFFFF) != BassLibrary.BASSVERSION) {
            throw new RuntimeException("BASS version is not correct");
        }
        if (!BassLibrary.INSTANCE.BASS_Init(this.captureAudioFormat == null ? -1 : 0, 48000, 0, 0, null)) {
            this.checkError("Failed to open device");
        }
        final BassLibrary.BASS_DEVICEINFO.ByReference deviceInfo = new BassLibrary.BASS_DEVICEINFO.ByReference();
        if (!BassLibrary.INSTANCE.BASS_GetDeviceInfo(BassLibrary.INSTANCE.BASS_GetDevice(), deviceInfo)) {
            this.checkError("Failed to get device info");
        }
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_SRC, 0)) { // linear interpolation
            this.checkError("Failed to set default sample rate conversion quality");
        }

        try {
            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.soundSamples.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }

        Runtime.getRuntime().addShutdownHook(this.shutdownHook = new Thread(() -> {
            this.shutdownHook = null;
            this.close();
        }));

        if (this.captureAudioFormat != null) {
            this.captureChannel = BassMixLibrary.INSTANCE.BASS_Mixer_StreamCreate((int) this.captureAudioFormat.getSampleRate(), this.captureAudioFormat.getChannels(), BassLibrary.BASS_STREAM_DECODE | BassLibrary.BASS_SAMPLE_FLOAT);
            this.checkError("Failed to create mixer stream");
            if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(this.captureChannel, BassMixLibrary.BASS_ATTRIB_MIXER_THREADS, Math.min(Runtime.getRuntime().availableProcessors(), 16))) {
                this.checkError("Failed to set mixer threads");
            }
            this.captureMemory = new Memory((long) this.captureAudioFormat.getSampleRate() * this.captureAudioFormat.getChannels() * 4 * 30);
            this.captureNormalizer = new NormalizationModifier();
        }

        final String versionString = "v" + ((version >> 24) & 0xFF) + "." + ((version >> 16) & 0xFF) + "." + ((version >> 8) & 0xFF) + "." + (version & 0xFF);
        System.out.println("Initialized BASS " + versionString + " on " + deviceInfo.name);
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.soundSamples.containsKey(sound)) return;

        if (this.playingChannels.size() >= this.maxSounds) {
            if (!BassLibrary.INSTANCE.BASS_ChannelFree(this.playingChannels.remove(0))) {
                this.checkError("Failed to free audio channel", BassLibrary.BASS_ERROR_HANDLE);
            }
        }

        final int channel = BassLibrary.INSTANCE.BASS_SampleGetChannel(this.soundSamples.get(sound), BassLibrary.BASS_SAMCHAN_STREAM | (this.captureAudioFormat == null ? BassLibrary.BASS_STREAM_AUTOFREE : BassLibrary.BASS_STREAM_DECODE));
        if (channel == 0) {
            this.checkError("Failed to get audio channel");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_VOL, volume)) {
            this.checkError("Failed to set audio channel volume");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_PAN, panning)) {
            this.checkError("Failed to set audio channel panning");
        }
        final FloatByReference freq = new FloatByReference();
        if (!BassLibrary.INSTANCE.BASS_ChannelGetAttribute(channel, BassLibrary.BASS_ATTRIB_FREQ, freq)) {
            this.checkError("Failed to get audio channel frequency");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_FREQ, freq.getValue() * pitch)) {
            this.checkError("Failed to set audio channel frequency");
        }
        final int sync = BassLibrary.INSTANCE.BASS_ChannelSetSync(channel, BassLibrary.BASS_SYNC_FREE, 0, this.channelFreeSync, null);
        if (sync == 0) {
            this.checkError("Failed to set audio channel end sync");
        }
        if (this.captureAudioFormat == null) {
            if (!BassLibrary.INSTANCE.BASS_ChannelStart(channel)) {
                this.checkError("Failed to play audio channel");
            }
        } else {
            if (!BassMixLibrary.INSTANCE.BASS_Mixer_StreamAddChannel(this.captureChannel, channel, BassLibrary.BASS_STREAM_AUTOFREE)) {
                this.checkError("Failed to add audio channel to mixer");
            }
        }
        this.playingChannels.add(channel);
    }

    public synchronized void renderSamples(final GrowableArray samples, final int sampleCount) {
        final int samplesLength = sampleCount * this.captureAudioFormat.getChannels();
        if (BassLibrary.INSTANCE.BASS_ChannelGetData(this.captureChannel, this.captureMemory, samplesLength * 4 | BassLibrary.BASS_DATA_FLOAT) < 0) {
            this.checkError("Failed to get audio data");
        }

        final float[] sampleData = this.captureMemory.getFloatArray(0, samplesLength);
        /*final int maxValue = (int) Math.pow(2, this.captureAudioFormat.getSampleSizeInBits() - 1) - 1;
        for (int i = 0; i < sampleData.length; i++) {
            sampleData[i] = (int) (Float.intBitsToFloat(sampleData[i]) * maxValue);
        }*/
        this.captureNormalizer.modify(this.captureAudioFormat, sampleData);
        samples.add(sampleData);
    }

    @Override
    public synchronized void stopSounds() {
        if (!BassLibrary.INSTANCE.BASS_Stop()) {
            this.checkError("Failed to stop sound system");
        }
        if (!BassLibrary.INSTANCE.BASS_Start()) {
            this.checkError("Failed to start sound system");
        }
        this.playingChannels.clear();
    }

    @Override
    public synchronized void close() {
        if (this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
        this.soundSamples.clear();
        this.playingChannels.clear();
        if (this.captureMemory != null) {
            this.captureMemory.close();
            this.captureMemory = null;
        }
        if (instance != null) {
            BassLibrary.INSTANCE.BASS_Free();
            instance = null;
        }
    }

    @Override
    public synchronized String getStatusLine() {
        return "Sounds: " + this.playingChannels.size() + " / " + this.maxSounds + ", BASS CPU Load: " + (int) BassLibrary.INSTANCE.BASS_GetCPU() + "%";
    }

    @Override
    public synchronized void setMasterVolume(final float volume) {
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_GVOL_STREAM, (int) (volume * 10000))) {
            this.checkError("Failed to set master volume");
        }
    }

    private int loadAudioFile(final byte[] data) {
        try {
            AudioInputStream audioInputStream = SoundFileUtil.readAudioFile(new ByteArrayInputStream(data));
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final AudioFormat targetFormat = new AudioFormat(audioFormat.getSampleRate(), 16, audioFormat.getChannels(), true, false);
            if (!audioFormat.matches(targetFormat)) audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            final byte[] audioBytes = IOUtil.readFully(audioInputStream);

            final int sample = BassLibrary.INSTANCE.BASS_SampleCreate(audioBytes.length, (int) audioFormat.getSampleRate(), audioFormat.getChannels(), 1, 0);
            if (sample == 0) {
                this.checkError("Failed to create sample");
            }
            if (!BassLibrary.INSTANCE.BASS_SampleSetData(sample, audioBytes)) {
                this.checkError("Failed to set sample data");
            }

            return sample;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load audio file", e);
        }
    }

    private void checkError(final String message, final int... allowedErrors) {
        final int error = BassLibrary.INSTANCE.BASS_ErrorGetCode();
        if (error != BassLibrary.BASS_OK) {
            for (int ignoreError : allowedErrors) {
                if (error == ignoreError) {
                    return;
                }
            }
            throw new RuntimeException("BASS error: " + message + " (" + error + ")");
        }
    }

}
