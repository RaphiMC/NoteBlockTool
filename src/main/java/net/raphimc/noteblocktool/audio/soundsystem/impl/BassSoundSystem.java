/*
 * This file is part of NoteBlockTool - https://github.com/RaphiMC/NoteBlockTool
 * Copyright (C) 2022-2024 RK_01/RaphiMC and contributors
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

import com.sun.jna.ptr.FloatByReference;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.BassLibrary;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class BassSoundSystem extends SoundSystem {

    private static BassSoundSystem instance;

    public static BassSoundSystem createPlayback(final int maxSounds) {
        if (instance != null) {
            throw new IllegalStateException("BASS sound system already initialized");
        }
        if (!BassLibrary.isLoaded()) {
            throw new IllegalStateException("BASS library is not available");
        }
        instance = new BassSoundSystem(maxSounds);
        return instance;
    }

    public static BassSoundSystem createCapture(final int maxSounds, final AudioFormat captureAudioFormat) {
        if (instance != null) {
            throw new IllegalStateException("BASS sound system already initialized");
        }
        if (!BassLibrary.isLoaded()) {
            throw new IllegalStateException("BASS library is not available");
        }
        instance = new BassSoundSystem(maxSounds);
        return instance;
    }


    private final Map<String, Integer> soundSamples = new HashMap<>();
    private Thread shutdownHook;

    private BassSoundSystem(final int maxSounds) {
        super(0);

        if (BassLibrary.INSTANCE.BASS_GetVersion() != BassLibrary.BASSVERSION) {
            throw new RuntimeException("BASS version is not correct");
        }
        if (!BassLibrary.INSTANCE.BASS_Init(-1, 44100, 0, 0, null)) {
            this.checkError("Could not open device");
        }
        final BassLibrary.BASS_DEVICEINFO.ByReference deviceInfo = new BassLibrary.BASS_DEVICEINFO.ByReference();
        if (!BassLibrary.INSTANCE.BASS_GetDeviceInfo(BassLibrary.INSTANCE.BASS_GetDevice(), deviceInfo)) {
            this.checkError("Could not get device info");
        }
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_SRC_SAMPLE, 1)) {
            this.checkError("Could not set default sample rate conversion quality");
        }

        try {
            for (Map.Entry<String, URL> entry : SoundMap.SOUND_LOCATIONS.entrySet()) {
                this.soundSamples.put(entry.getKey(), this.loadAudioFile(entry.getValue().openStream(), maxSounds));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Could not load sound samples", e);
        }

        Runtime.getRuntime().addShutdownHook(this.shutdownHook = new Thread(() -> {
            this.shutdownHook = null;
            this.close();
        }));

        System.out.println("Initialized BASS on " + deviceInfo.name);
    }

    @Override
    public void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.soundSamples.containsKey(sound)) return;

        final int channel = BassLibrary.INSTANCE.BASS_SampleGetChannel(this.soundSamples.get(sound), BassLibrary.BASS_SAMPLE_OVER_VOL);
        if (channel == 0) {
            this.checkError("Could not get audio channel");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_VOL, volume)) {
            this.checkError("Could not set audio channel volume");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_PAN, panning)) {
            this.checkError("Could not set audio channel panning");
        }
        final FloatByReference freq = new FloatByReference();
        if (!BassLibrary.INSTANCE.BASS_ChannelGetAttribute(channel, BassLibrary.BASS_ATTRIB_FREQ, freq)) {
            this.checkError("Could not get audio channel frequency");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelSetAttribute(channel, BassLibrary.BASS_ATTRIB_FREQ, freq.getValue() * pitch)) {
            this.checkError("Could not set audio channel frequency");
        }
        if (!BassLibrary.INSTANCE.BASS_ChannelStart(channel)) {
            this.checkError("Could not play audio channel");
        }
    }

    @Override
    public void stopSounds() {
        if (!BassLibrary.INSTANCE.BASS_Stop()) {
            this.checkError("Could not stop sound system");
        }
        if (!BassLibrary.INSTANCE.BASS_Start()) {
            this.checkError("Could not start sound system");
        }
    }

    @Override
    public void close() {
        if (this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
        this.soundSamples.clear();
        if (instance != null) {
            BassLibrary.INSTANCE.BASS_Free();
            instance = null;
        }
    }

    @Override
    public String getStatusLine() {
        return "CPU Load: " + (int) BassLibrary.INSTANCE.BASS_GetCPU() + "%";
    }

    @Override
    public void setMasterVolume(final float volume) {
        if (!BassLibrary.INSTANCE.BASS_SetConfig(BassLibrary.BASS_CONFIG_GVOL_SAMPLE, (int) (volume * 10000))) {
            this.checkError("Could not set master volume");
        }
    }

    private int loadAudioFile(final InputStream inputStream, final int maxSounds) {
        try {
            AudioInputStream audioInputStream = SoundSampleUtil.readAudioFile(inputStream);
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final AudioFormat targetFormat = new AudioFormat(audioFormat.getSampleRate(), 16, audioFormat.getChannels(), true, false);
            if (!audioFormat.matches(targetFormat)) audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            final byte[] audioBytes = IOUtil.readFully(audioInputStream);

            final int sample = BassLibrary.INSTANCE.BASS_SampleCreate(audioBytes.length, (int) audioFormat.getSampleRate(), audioFormat.getChannels(), maxSounds, 0);
            if (sample == 0) {
                this.checkError("Could not create sample");
            }
            if (!BassLibrary.INSTANCE.BASS_SampleSetData(sample, audioBytes)) {
                this.checkError("Could not set sample data");
            }

            return sample;
        } catch (Throwable e) {
            throw new RuntimeException("Could not load audio file", e);
        }
    }

    private void checkError(final String message) {
        final int error = BassLibrary.INSTANCE.BASS_ErrorGetCode();
        if (error != BassLibrary.BASS_OK) {
            throw new RuntimeException("BASS error: " + message + " (" + error + ")");
        }
    }

}
