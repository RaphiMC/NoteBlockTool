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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SoundSampleUtil {

    public static int[] readSamples(final InputStream inputStream, final AudioFormat targetFormat) throws UnsupportedAudioFileException, IOException {
        AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
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
        for (int i = 0; i < newSamples.length; i++) {
            // Long to prevent clipping of the index
            final long index = (long) i * samples.length / newSamples.length;
            newSamples[i] = (int) (samples[(int) index] * volume);
        }
        return newSamples;
    }

}
