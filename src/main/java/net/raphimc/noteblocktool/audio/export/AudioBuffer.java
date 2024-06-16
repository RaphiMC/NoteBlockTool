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
package net.raphimc.noteblocktool.audio.export;

import net.raphimc.noteblocktool.util.SoundSampleUtil;

public class AudioBuffer {

    private long[] samples;
    private int sampleIndex;

    public AudioBuffer(final int initialSize) {
        this.samples = new long[initialSize];
    }

    public void pushSamples(final int[] samples) {
        if (this.sampleIndex + samples.length >= this.samples.length) {
            final long[] newSamples = new long[this.sampleIndex + samples.length];
            System.arraycopy(this.samples, 0, newSamples, 0, this.samples.length);
            this.samples = newSamples;
        }

        for (int i = 0; i < samples.length; i++) {
            this.samples[this.sampleIndex + i] += samples[i];
        }
    }

    public void advanceIndex(final int samples) {
        this.sampleIndex += samples;
    }

    public byte[] normalizeBytes() {
        this.normalize(Byte.MAX_VALUE);
        final byte[] bytes = new byte[this.samples.length];
        for (int i = 0; i < this.samples.length; i++) {
            bytes[i] = (byte) this.samples[i];
        }
        return bytes;
    }

    public short[] normalizeShorts() {
        this.normalize(Short.MAX_VALUE);
        final short[] shorts = new short[this.samples.length];
        for (int i = 0; i < this.samples.length; i++) {
            shorts[i] = (short) this.samples[i];
        }
        return shorts;
    }

    public int[] normalizeInts() {
        this.normalize(Integer.MAX_VALUE);
        final int[] ints = new int[this.samples.length];
        for (int i = 0; i < this.samples.length; i++) {
            ints[i] = (int) this.samples[i];
        }
        return ints;
    }

    private void normalize(final long maxValue) {
        final long max = SoundSampleUtil.getMax(this.samples);
        final float factor = (float) maxValue / max;
        for (int i = 0; i < this.samples.length; i++) {
            this.samples[i] = (long) (this.samples[i] * factor);
        }
    }

}
