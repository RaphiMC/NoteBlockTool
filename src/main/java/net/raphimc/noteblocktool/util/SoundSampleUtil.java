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
package net.raphimc.noteblocktool.util;

import com.google.common.io.ByteStreams;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class SoundSampleUtil {

    private static final byte[] OGG_MAGIC = new byte[]{(byte) 'O', (byte) 'g', (byte) 'g', (byte) 'S'};

    public static AudioInputStream readAudioFile(final InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        final BufferedInputStream bis = new BufferedInputStream(inputStream);
        final byte[] magic = new byte[4];
        bis.mark(magic.length);
        bis.read(magic);
        bis.reset();
        if (Arrays.equals(magic, OGG_MAGIC)) {
            final byte[] data = ByteStreams.toByteArray(bis);
            final ByteBuffer dataBuffer = (ByteBuffer) MemoryUtil.memAlloc(data.length).put(data).flip();
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                final IntBuffer channels = memoryStack.callocInt(1);
                final IntBuffer sampleRate = memoryStack.callocInt(1);
                final PointerBuffer samples = memoryStack.callocPointer(1);

                final int samplesCount = STBVorbis.stb_vorbis_decode_memory(dataBuffer, channels, sampleRate, samples);
                if (samplesCount == -1) {
                    MemoryUtil.memFree(dataBuffer);
                    throw new RuntimeException("Failed to decode ogg file");
                }

                final ByteBuffer samplesBuffer = samples.getByteBuffer(samplesCount * 2);
                final byte[] samplesArray = new byte[samplesCount * 2];
                samplesBuffer.get(samplesArray);

                MemoryUtil.memFree(dataBuffer);
                MemoryUtil.memFree(samplesBuffer);

                final AudioFormat audioFormat = new AudioFormat(sampleRate.get(), 16, channels.get(), true, false);
                return new AudioInputStream(new ByteArrayInputStream(samplesArray), audioFormat, samplesArray.length);
            }
        } else {
            return AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
        }
    }

    public static int[] readSamples(final InputStream inputStream, final AudioFormat targetFormat) throws UnsupportedAudioFileException, IOException {
        AudioInputStream in = readAudioFile(inputStream);
        if (!in.getFormat().matches(targetFormat)) in = AudioSystem.getAudioInputStream(targetFormat, in);
        final byte[] audioBytes = ByteStreams.toByteArray(in);
        final SampleInputStream sis = new SampleInputStream(new ByteArrayInputStream(audioBytes), targetFormat);

        final int sampleSize = targetFormat.getSampleSizeInBits() / 8;
        final int[] samples = new int[audioBytes.length / sampleSize];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = sis.readSample();
        }

        return samples;
    }

    public static int[] mutate(final int[] samples, final float volume, final float pitchChangeFactor) {
        final int[] newSamples = new int[(int) (samples.length / pitchChangeFactor)];
        if (pitchChangeFactor < 1) {
            //Interpolate the samples for better quality
            for (int i = 0; i < newSamples.length; i++) {
                final float index = i * pitchChangeFactor;
                final int lowerIndex = (int) index;
                final int upperIndex = lowerIndex + 1;
                final float fraction = index - lowerIndex;
                if (upperIndex < samples.length) {
                    newSamples[i] = (int) ((1 - fraction) * samples[lowerIndex] + fraction * samples[upperIndex]);
                } else {
                    newSamples[i] = samples[lowerIndex];
                }
                newSamples[i] = (int) (newSamples[i] * volume);
            }
        } else {
            for (int i = 0; i < newSamples.length; i++) {
                // Long to prevent clipping of the index
                final long index = (long) i * samples.length / newSamples.length;
                newSamples[i] = (int) (samples[(int) index] * volume);
            }
        }
        return newSamples;
    }

    public static long getMax(final long[] samples) {
        long max = 1;
        for (long sample : samples) max = Math.max(max, Math.abs(sample));
        return max;
    }

}
