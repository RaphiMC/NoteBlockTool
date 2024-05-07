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
package net.raphimc.noteblocktool.audio;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.OutputStream;

public class SampleOutputStream extends OutputStream {

    private final OutputStream outputStream;
    private final AudioFormat audioFormat;

    public SampleOutputStream(final OutputStream outputStream, final AudioFormat audioFormat) {
        if (audioFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && audioFormat.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
        }
        this.outputStream = outputStream;
        this.audioFormat = audioFormat;
    }

    @Override
    public void write(final int b) throws IOException {
        this.outputStream.write(b);
    }

    public void writeSample(final int sample) throws IOException {
        switch (this.audioFormat.getSampleSizeInBits()) {
            case 8:
                this.write(sample);
                break;
            case 16:
                this.write16Bit(sample);
                break;
            case 32:
                this.write32Bit(sample);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported sample size: " + this.audioFormat.getSampleSizeInBits());
        }
    }

    private void write16Bit(final int sample) throws IOException {
        if (this.audioFormat.isBigEndian()) {
            this.write((sample >> 8) & 0xFF);
            this.write(sample & 0xFF);
        } else {
            this.write(sample & 0xFF);
            this.write((sample >> 8) & 0xFF);
        }
    }

    private void write32Bit(final int sample) throws IOException {
        if (this.audioFormat.isBigEndian()) {
            this.write((sample >> 24) & 0xFF);
            this.write((sample >> 16) & 0xFF);
            this.write((sample >> 8) & 0xFF);
            this.write(sample & 0xFF);
        } else {
            this.write(sample & 0xFF);
            this.write((sample >> 8) & 0xFF);
            this.write((sample >> 16) & 0xFF);
            this.write((sample >> 24) & 0xFF);
        }
    }

}
