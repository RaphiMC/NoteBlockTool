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
package net.raphimc.noteblocktool.audio.soundsystem;

import com.google.common.io.ByteStreams;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocktool.audio.SoundMap;
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
import java.util.HashMap;
import java.util.Map;

public class JavaxSoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);
    private static Map<Instrument, int[]> sounds;
    private static int samplesPerTick;
    private static SourceDataLine dataLine;
    private static long[] buffer = new long[0];
    private static Map<String, int[]> mutationCache;

    public static void init(final float playbackSpeed) {
        try {
            sounds = loadSounds();

            samplesPerTick = (int) (FORMAT.getSampleRate() / playbackSpeed);
            dataLine = AudioSystem.getSourceDataLine(FORMAT);
            dataLine.open(FORMAT, (int) FORMAT.getSampleRate());
            dataLine.start();
            mutationCache = new HashMap<>();
        } catch (Throwable t) {
            throw new RuntimeException("Could not initialize audio system", t);
        }
    }

    public static void destroy() {
        dataLine.stop();
        sounds = null;
        buffer = new long[0];
        mutationCache = null;
    }

    public static void playNote(final Instrument instrument, final float volume, final float pitch) {
        String key = instrument.name() + "\0" + volume + "\0" + pitch;
        int[] samples = mutationCache.computeIfAbsent(key, k -> SoundSampleUtil.mutate(sounds.get(instrument), volume, pitch));
        if (buffer.length < samples.length) buffer = Arrays.copyOf(buffer, samples.length);
        for (int i = 0; i < samples.length; i++) buffer[i] += samples[i];
    }

    public static void tick() {
        long[] samples = Arrays.copyOfRange(buffer, 0, samplesPerTick);
        dataLine.write(write(samples), 0, samples.length * 2);
        if (buffer.length > samplesPerTick) buffer = Arrays.copyOfRange(buffer, samplesPerTick, buffer.length);
        else if (buffer.length != 0) buffer = new long[0];
    }

    public static void flushDataLine() {
        dataLine.flush();
    }

    private static Map<Instrument, int[]> loadSounds() {
        try {
            Map<Instrument, int[]> sounds = new HashMap<>();
            for (Map.Entry<Instrument, String> entry : SoundMap.SOUNDS.entrySet()) {
                sounds.put(entry.getKey(), readSound(JavaxSoundSystem.class.getResourceAsStream(entry.getValue())));
            }
            return sounds;
        } catch (Throwable e) {
            throw new RuntimeException("Could not load audio buffer", e);
        }
    }

    private static int[] readSound(final InputStream is) {
        try {
            AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
            if (!in.getFormat().matches(FORMAT)) in = AudioSystem.getAudioInputStream(FORMAT, in);
            final byte[] audioBytes = ByteStreams.toByteArray(in);

            final int sampleSize = FORMAT.getSampleSizeInBits() / 8;
            final int[] samples = new int[audioBytes.length / sampleSize];
            for (int i = 0; i < samples.length; i++) {
                final byte[] sampleBytes = new byte[sampleSize];
                System.arraycopy(audioBytes, i * sampleSize, sampleBytes, 0, sampleSize);
                samples[i] = ByteBuffer.wrap(sampleBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
            }

            return samples;
        } catch (Throwable t) {
            throw new RuntimeException("Could not read sound", t);
        }
    }

    private static byte[] write(final long[] samples) {
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
