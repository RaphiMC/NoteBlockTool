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
package net.raphimc.noteblocktool.audio;

import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import net.raphimc.noteblocktool.util.CircularBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class OggAudioInputStream extends InputStream {

    private static final int BUFFER_SIZE = 8192; // Don't change. This is hardcoded in jorbis.

    private final SyncState syncState = new SyncState();
    private final Page page = new Page();
    private final StreamState streamState = new StreamState();
    private final Packet packet = new Packet();
    private final Info info = new Info();
    private final DspState dspState = new DspState();
    private final Block block = new Block(this.dspState);
    private final InputStream oggStream;
    private CircularBuffer samplesBuffer;
    private long totalSamples = Long.MAX_VALUE;
    private long writtenSamples;

    public static AudioInputStream create(final InputStream oggStream) throws IOException {
        final OggAudioInputStream oggAudioInputStream = new OggAudioInputStream(oggStream);

        final Comment comment = new Comment();
        final Page page = oggAudioInputStream.readPage();
        if (page == null) {
            throw new IOException("Invalid ogg file: Can't read first page");
        }

        Packet packet = oggAudioInputStream.readIdentificationPacket(page);
        if (oggAudioInputStream.info.synthesis_headerin(comment, packet) < 0) {
            throw new IOException("Invalid ogg identification packet");
        }

        for (int i = 0; i < 2; i++) {
            packet = oggAudioInputStream.readPacket();
            if (packet == null) {
                throw new EOFException("Unexpected end of ogg stream");
            } else if (oggAudioInputStream.info.synthesis_headerin(comment, packet) < 0) {
                throw new IOException("Invalid ogg header packet " + i);
            }
        }

        oggAudioInputStream.dspState.synthesis_init(oggAudioInputStream.info);
        oggAudioInputStream.block.init(oggAudioInputStream.dspState);
        oggAudioInputStream.samplesBuffer = new CircularBuffer(BUFFER_SIZE * Short.BYTES * oggAudioInputStream.info.channels * 10);

        final AudioFormat audioFormat = new AudioFormat(oggAudioInputStream.info.rate, Short.SIZE, oggAudioInputStream.info.channels, true, false);
        return new AudioInputStream(oggAudioInputStream, audioFormat, AudioSystem.NOT_SPECIFIED);
    }

    private OggAudioInputStream(final InputStream oggStream) {
        this.oggStream = oggStream;
    }

    @Override
    public int read() throws IOException {
        while (this.fillSamplesBuffer(1)) {
        }

        if (this.samplesBuffer.isEmpty()) {
            return -1;
        } else {
            return this.samplesBuffer.takeSafe() & 0xFF;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        while (this.fillSamplesBuffer(len)) {
        }

        if (this.samplesBuffer.isEmpty()) {
            return -1;
        } else {
            final byte[] data = this.samplesBuffer.takeAllSafe(Math.min(len, this.samplesBuffer.getCurrentSize()));
            System.arraycopy(data, 0, b, off, data.length);
            return data.length;
        }
    }

    @Override
    public void close() throws IOException {
        this.oggStream.close();
    }

    private boolean fillSamplesBuffer(final int minSize) throws IOException {
        if (this.samplesBuffer.getCurrentSize() >= minSize) {
            return false;
        }

        final Packet packet = this.readPacket();
        if (packet == null) {
            return false;
        } else if (this.block.synthesis(packet) < 0) {
            throw new IOException("Can't decode audio packet");
        } else {
            this.dspState.synthesis_blockin(this.block);
            final float[][][] allSamples = new float[1][][];
            final int[] offsets = new int[this.info.channels];

            int sampleCount;
            while ((sampleCount = this.dspState.synthesis_pcmout(allSamples, offsets)) > 0) {
                final int actualSampleCount = (int) Math.min(sampleCount, this.totalSamples - this.writtenSamples);

                for (int i = 0; i < actualSampleCount; i++) {
                    for (int channel = 0; channel < this.info.channels; channel++) {
                        final int offset = offsets[channel];
                        final float[] samples = allSamples[0][channel];
                        final float floatSample = samples[offset + i];
                        final short sample = (short) (floatSample > 0 ? (floatSample * Short.MAX_VALUE) : (floatSample * (-Short.MIN_VALUE)));
                        this.samplesBuffer.add((byte) (sample & 0xFF));
                        this.samplesBuffer.add((byte) ((sample >> 8) & 0xFF));
                    }
                }

                this.writtenSamples += actualSampleCount;
                this.dspState.synthesis_read(sampleCount);
            }

            return true;
        }
    }

    private Packet readIdentificationPacket(final Page page) throws IOException {
        this.streamState.init(page.serialno());
        if (this.streamState.pagein(page) < 0) {
            throw new IOException("Failed to parse page");
        } else {
            final int result = this.streamState.packetout(this.packet);
            if (result != 1) {
                throw new IOException("Failed to read identification packet: " + result);
            } else {
                return this.packet;
            }
        }
    }

    private Page readPage() throws IOException {
        while (true) {
            final int result = this.syncState.pageout(this.page);
            switch (result) {
                case -1 -> throw new IllegalStateException("Corrupt or missing data in ogg stream");
                case 0 -> {
                    final int offset = this.syncState.buffer(BUFFER_SIZE);
                    final int size = this.oggStream.read(this.syncState.data, offset, BUFFER_SIZE);
                    if (size == -1) {
                        return null;
                    } else {
                        this.syncState.wrote(size);
                    }
                }
                case 1 -> {
                    if (this.page.eos() != 0) {
                        this.totalSamples = this.page.granulepos();
                    }

                    return this.page;
                }
                default -> throw new IllegalStateException("Unknown page decode result: " + result);
            }
        }
    }

    private Packet readPacket() throws IOException {
        while (true) {
            final int result = this.streamState.packetout(this.packet);
            switch (result) {
                case -1 -> throw new IOException("Failed to parse packet");
                case 0 -> {
                    final Page page = this.readPage();
                    if (page == null) {
                        return null;
                    } else if (this.streamState.pagein(page) < 0) {
                        throw new IOException("Failed to parse page");
                    }
                }
                case 1 -> {
                    return this.packet;
                }
                default -> throw new IllegalStateException("Unknown packet decode result: " + result);
            }
        }
    }

}
