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
package net.raphimc.noteblocktool.audio.system.impl;

import com.sun.jna.Memory;
import com.sun.jna.ptr.FloatByReference;
import net.raphimc.audiomixer.soundmodifier.impl.NormalizationModifier;
import net.raphimc.audiomixer.util.MathUtil;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocktool.audio.library.BassLibrary;
import net.raphimc.noteblocktool.audio.library.BassMixLibrary;
import net.raphimc.noteblocktool.audio.system.AudioSystem;
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SoundFileUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassAudioSystem extends AudioSystem {

    static {
        if (!BassLibrary.isLoaded()) {
            throw new IllegalStateException("BASS library is not available");
        }
        if (!BassMixLibrary.isLoaded()) {
            throw new IllegalStateException("BASS mix library is not available");
        }
    }

    private final Map<String, Integer> soundSamples = new HashMap<>();
    private final List<Integer> playingChannels = new ArrayList<>();
    private int loopbackChannel;
    private Memory loopbackMemory;
    private NormalizationModifier loopbackNormalizer;

    @SuppressWarnings("FieldCanBeLocal")
    private final BassLibrary.SYNCPROC channelFreeSync = (handle, channel, data, user) -> {
        synchronized (this) {
            this.playingChannels.remove((Integer) channel);
        }
    };

    public BassAudioSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        super(soundData, maxSounds);

        final int version = BassLibrary.INSTANCE.BASS_GetVersion();
        if (((version >> 16) & 0xFFFF) != BassLibrary.BASSVERSION) {
            throw new RuntimeException("BASS version is not correct");
        }
        if (!BassLibrary.INSTANCE.BASS_Init(-1, 48000, 0, 0, null)) {
            this.checkError("Failed to open device");
        }
        final BassLibrary.BASS_DEVICEINFO.ByReference deviceInfo = new BassLibrary.BASS_DEVICEINFO.ByReference();
        if (!BassLibrary.INSTANCE.BASS_GetDeviceInfo(BassLibrary.INSTANCE.BASS_GetDevice(), deviceInfo)) {
            this.checkError("Failed to get device info");
        }
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_SRC, 0)) { // linear interpolation
            this.checkError("Failed to set default sample rate conversion quality");
        }

        for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
            this.soundSamples.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
        }

        final String versionString = "v" + ((version >> 24) & 0xFF) + "." + ((version >> 16) & 0xFF) + "." + ((version >> 8) & 0xFF) + "." + (version & 0xFF);
        System.out.println("Initialized BASS " + versionString + " on " + deviceInfo.name);
    }

    public BassAudioSystem(final Map<String, byte[]> soundData, final int maxSounds, final PcmFloatAudioFormat loopbackAudioFormat) {
        super(soundData, maxSounds, loopbackAudioFormat);

        final int version = BassLibrary.INSTANCE.BASS_GetVersion();
        if (((version >> 16) & 0xFFFF) != BassLibrary.BASSVERSION) {
            throw new RuntimeException("BASS version is not correct");
        }
        if (!BassLibrary.INSTANCE.BASS_Init(0, 48000, 0, 0, null)) {
            this.checkError("Failed to open device");
        }
        final BassLibrary.BASS_DEVICEINFO.ByReference deviceInfo = new BassLibrary.BASS_DEVICEINFO.ByReference();
        if (!BassLibrary.INSTANCE.BASS_GetDeviceInfo(BassLibrary.INSTANCE.BASS_GetDevice(), deviceInfo)) {
            this.checkError("Failed to get device info");
        }
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_SRC, 0)) { // linear interpolation
            this.checkError("Failed to set default sample rate conversion quality");
        }

        this.loopbackChannel = BassMixLibrary.INSTANCE.BASS_Mixer_StreamCreate((int) loopbackAudioFormat.getSampleRate(), loopbackAudioFormat.getChannels(), BassLibrary.BASS_STREAM_DECODE | BassLibrary.BASS_SAMPLE_FLOAT);
        this.checkError("Failed to create mixer stream");
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(this.loopbackChannel, BassMixLibrary.BASS_ATTRIB_MIXER_THREADS, Math.min(Runtime.getRuntime().availableProcessors(), 16))) {
            this.checkError("Failed to set mixer threads");
        }
        this.loopbackMemory = new Memory(MathUtil.millisToByteCount(loopbackAudioFormat, 5000));
        this.loopbackNormalizer = new NormalizationModifier();

        for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
            this.soundSamples.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
        }

        final String versionString = "v" + ((version >> 24) & 0xFF) + "." + ((version >> 16) & 0xFF) + "." + ((version >> 8) & 0xFF) + "." + (version & 0xFF);
        System.out.println("Initialized BASS " + versionString + " on " + deviceInfo.name);
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.soundSamples.containsKey(sound)) return;

        if (this.playingChannels.size() >= this.getMaxSounds()) {
            if (!BassLibrary.INSTANCE.BASS_ChannelFree(this.playingChannels.remove(0))) {
                this.checkError("Failed to free audio channel", BassLibrary.BASS_ERROR_HANDLE);
            }
        }

        final int channel = BassLibrary.INSTANCE.BASS_SampleGetChannel(this.soundSamples.get(sound), BassLibrary.BASS_SAMCHAN_STREAM | (this.getLoopbackAudioFormat() == null ? BassLibrary.BASS_STREAM_AUTOFREE : BassLibrary.BASS_STREAM_DECODE));
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
        if (this.getLoopbackAudioFormat() == null) {
            if (!BassLibrary.INSTANCE.BASS_ChannelStart(channel)) {
                this.checkError("Failed to play audio channel");
            }
        } else {
            if (!BassMixLibrary.INSTANCE.BASS_Mixer_StreamAddChannel(this.loopbackChannel, channel, BassLibrary.BASS_STREAM_AUTOFREE)) {
                this.checkError("Failed to add audio channel to mixer");
            }
        }
        this.playingChannels.add(channel);
    }

    @Override
    public synchronized void stopAllSounds() {
        if (!BassLibrary.INSTANCE.BASS_Stop()) {
            this.checkError("Failed to stop audio system");
        }
        if (!BassLibrary.INSTANCE.BASS_Start()) {
            this.checkError("Failed to start audio system");
        }
        this.playingChannels.clear();
    }

    @Override
    public synchronized float[] render(final int frameCount) {
        final int samplesLength = frameCount * this.getLoopbackAudioFormat().getChannels();
        if ((long) samplesLength * Float.BYTES > this.loopbackMemory.size()) {
            throw new IllegalStateException("Loopback memory is too small");
        }
        if (BassLibrary.INSTANCE.BASS_ChannelGetData(this.loopbackChannel, this.loopbackMemory, samplesLength * Float.BYTES | BassLibrary.BASS_DATA_FLOAT) < 0) {
            this.checkError("Failed to get audio data");
        }
        final float[] samples = this.loopbackMemory.getFloatArray(0, samplesLength);
        this.loopbackNormalizer.modify(this.getLoopbackAudioFormat(), samples);
        return samples;
    }

    @Override
    public synchronized void close() {
        this.soundSamples.clear();
        this.playingChannels.clear();
        if (this.loopbackMemory != null) {
            this.loopbackMemory.close();
            this.loopbackMemory = null;
        }
        BassLibrary.INSTANCE.BASS_Free();
    }

    @Override
    public void setMasterVolume(final float volume) {
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_GVOL_STREAM, (int) (volume * 10000))) {
            this.checkError("Failed to set master volume");
        }
    }

    @Override
    public synchronized Integer getPlayingSounds() {
        return this.playingChannels.size();
    }

    @Override
    public Float getCpuLoad() {
        return BassLibrary.INSTANCE.BASS_GetCPU();
    }

    private int loadAudioFile(final byte[] data) {
        try {
            AudioInputStream audioInputStream = SoundFileUtil.readAudioFile(new ByteArrayInputStream(data));
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final AudioFormat targetFormat = new AudioFormat(audioFormat.getSampleRate(), Short.SIZE, audioFormat.getChannels(), true, false);
            if (!audioFormat.matches(targetFormat)) audioInputStream = javax.sound.sampled.AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
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
