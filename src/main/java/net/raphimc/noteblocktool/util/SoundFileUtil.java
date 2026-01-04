/*
 * This file is part of NoteBlockTool - https://github.com/RaphiMC/NoteBlockTool
 * Copyright (C) 2022-2026 RK_01/RaphiMC and contributors
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

import net.raphimc.audiomixer.io.ogg.OggVorbisInputStream;
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

public class SoundFileUtil {

    private static final byte[] OGG_MAGIC = new byte[]{(byte) 'O', (byte) 'g', (byte) 'g', (byte) 'S'};

    public static AudioInputStream readAudioFile(final InputStream inputStream) throws UnsupportedAudioFileException, IOException {
        final BufferedInputStream bis = new BufferedInputStream(inputStream);
        final byte[] magic = new byte[4];
        bis.mark(magic.length);
        bis.read(magic);
        bis.reset();
        if (Arrays.equals(magic, OGG_MAGIC)) {
            final byte[] data = IOUtil.readFully(bis);
            try {
                final ByteBuffer dataBuffer = MemoryUtil.memAlloc(data.length).put(data).flip();
                try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                    final IntBuffer channels = memoryStack.callocInt(1);
                    final IntBuffer sampleRate = memoryStack.callocInt(1);
                    final PointerBuffer samples = memoryStack.callocPointer(1);

                    final int samplesCount = STBVorbis.stb_vorbis_decode_memory(dataBuffer, channels, sampleRate, samples);
                    if (samplesCount == -1) {
                        MemoryUtil.memFree(dataBuffer);
                        throw new RuntimeException("Failed to decode ogg file");
                    }

                    final ByteBuffer samplesBuffer = samples.getByteBuffer(samplesCount * Short.BYTES);
                    final byte[] samplesArray = new byte[samplesCount * Short.BYTES];
                    samplesBuffer.get(samplesArray);

                    MemoryUtil.memFree(dataBuffer);
                    MemoryUtil.memFree(samplesBuffer);

                    final AudioFormat audioFormat = new AudioFormat(sampleRate.get(), Short.SIZE, channels.get(), true, false);
                    return new AudioInputStream(new ByteArrayInputStream(samplesArray), audioFormat, samplesArray.length);
                }
            } catch (Throwable e) { // Fallback if natives aren't available or if STB Vorbis fails to parse the file
                System.err.println("Failed to decode ogg file using STB Vorbis, falling back to JOrbis");
                e.printStackTrace();
                return OggVorbisInputStream.createAudioInputStream(new ByteArrayInputStream(data));
            }
        } else {
            return AudioSystem.getAudioInputStream(bis);
        }
    }

}
