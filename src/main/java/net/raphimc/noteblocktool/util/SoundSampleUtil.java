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

public class SoundSampleUtil {

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

}
