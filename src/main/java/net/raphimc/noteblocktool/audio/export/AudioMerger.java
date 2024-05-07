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

public class AudioMerger {

    private final long[] samples;
    private int sampleIndex;

    public AudioMerger(final int sampleCount) {
        this.samples = new long[sampleCount];
    }

    public long[] getSamples() {
        return this.samples;
    }

    public void addSamples(final int[] samples) {
        for (int i = 0; i < samples.length; i++) {
            int index = this.sampleIndex + i;
            if (index >= this.samples.length) break;
            int sample = samples[i];
            this.samples[index] += sample;
        }
    }

    public void pushSamples(final int samples) {
        this.sampleIndex += samples;
    }

    public byte[] normalizeBytes() {
        byte[] bytes = new byte[this.samples.length];
        long max = this.getMax();
        for (int i = 0; i < this.samples.length; i++) {
            bytes[i] = (byte) (this.samples[i] * Byte.MAX_VALUE / max);
        }
        return bytes;
    }

    public short[] normalizeShorts() {
        short[] shorts = new short[this.samples.length];
        long max = this.getMax();
        for (int i = 0; i < this.samples.length; i++) {
            shorts[i] = (short) (this.samples[i] * Short.MAX_VALUE / max);
        }
        return shorts;
    }

    public int[] normalizeInts() {
        int[] ints = new int[this.samples.length];
        long max = this.getMax();
        for (int i = 0; i < this.samples.length; i++) {
            ints[i] = (int) (this.samples[i] * Integer.MAX_VALUE / max);
        }
        return ints;
    }

    private long getMax() {
        long max = 0;
        for (long sample : this.samples) max = Math.max(max, Math.abs(sample));
        return max;
    }

}
