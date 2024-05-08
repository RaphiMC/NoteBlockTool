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

import com.google.common.io.ByteStreams;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class JavaxSoundSystem extends SoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);

    private final Map<Instrument, int[]> sounds;
    private final int samplesPerTick;
    private final SourceDataLine dataLine;
    private final Map<String, int[]> mutationCache;
    private long[] buffer = new long[0];

    public JavaxSoundSystem(final int maxSounds, final float playbackSpeed) {
        super(maxSounds);

        try {
            this.sounds = this.loadSounds();
            this.samplesPerTick = (int) (FORMAT.getSampleRate() / playbackSpeed);
            this.dataLine = AudioSystem.getSourceDataLine(FORMAT);
            this.dataLine.open(FORMAT, (int) FORMAT.getSampleRate());
            this.dataLine.start();
            this.mutationCache = new HashMap<>();
        } catch (Throwable e) {
            throw new RuntimeException("Could not initialize javax audio system", e);
        }
    }

    @Override
    public void playNote(Instrument instrument, float volume, float pitch, float panning) {
        String key = instrument.ordinal() + "\0" + volume + "\0" + pitch;
        int[] samples = this.mutationCache.computeIfAbsent(key, k -> SoundSampleUtil.mutate(this.sounds.get(instrument), volume * this.masterVolume, pitch));
        if (this.buffer.length < samples.length) this.buffer = Arrays.copyOf(this.buffer, samples.length);
        for (int i = 0; i < samples.length; i++) this.buffer[i] += samples[i];
    }

    @Override
    public void writeSamples() {
        long[] samples = Arrays.copyOfRange(this.buffer, 0, this.samplesPerTick);
        this.dataLine.write(this.write(samples), 0, samples.length * 2);
        if (this.buffer.length > this.samplesPerTick) this.buffer = Arrays.copyOfRange(this.buffer, this.samplesPerTick, this.buffer.length);
        else if (this.buffer.length != 0) this.buffer = new long[0];
    }

    @Override
    public void stopSounds() {
        this.dataLine.flush();
    }

    @Override
    public void close() {
        this.dataLine.stop();
    }

    @Override
    public void setMasterVolume(float volume) {
        super.setMasterVolume(volume);
        this.mutationCache.clear();
    }

    @Override
    public int getMaxSounds() {
        return 0;
    }

    @Override
    public int getSoundCount() {
        return 0;
    }

    private Map<Instrument, int[]> loadSounds() {
        try {
            Map<Instrument, int[]> sounds = new EnumMap<>(Instrument.class);
            for (Map.Entry<Instrument, String> entry : SoundMap.SOUNDS.entrySet()) {
                sounds.put(entry.getKey(), this.readSound(JavaxSoundSystem.class.getResourceAsStream(entry.getValue())));
            }
            return sounds;
        } catch (Throwable e) {
            throw new RuntimeException("Could not load audio buffer", e);
        }
    }

    private int[] readSound(final InputStream is) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            if (!in.getFormat().matches(FORMAT)) in = AudioSystem.getAudioInputStream(FORMAT, in);
            final byte[] audioBytes = ByteStreams.toByteArray(in);

            final int sampleSize = FORMAT.getSampleSizeInBits() / 8;
            final int[] samples = new int[audioBytes.length / sampleSize];
            for (int i = 0; i < samples.length; i++) {
                samples[i] = ByteBuffer.wrap(audioBytes, i * sampleSize, sampleSize).order(ByteOrder.LITTLE_ENDIAN).getShort();
            }

            return samples;
        } catch (Throwable t) {
            throw new RuntimeException("Could not read sound", t);
        }
    }

    private byte[] write(final long[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            long sample = samples[i];
            if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
            else if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;

            short conv = (short) sample;
            out[i * 2] = (byte) (conv & 0xFF);
            out[i * 2 + 1] = (byte) ((conv >> 8) & 0xFF);
        }
        return out;
    }

}
