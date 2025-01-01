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
package net.raphimc.noteblocktool.audio.export;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.util.HashMap;
import java.util.Map;

public interface LameLibrary extends Library {

    LameLibrary INSTANCE = loadNative();

    static LameLibrary loadNative() {
        try {
            final Map<String, Object> options = new HashMap<>();
            options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
            return Native.load("mp3lame", LameLibrary.class, options);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    String get_lame_version();

    Pointer lame_init();

    int lame_set_in_samplerate(final Pointer lame, final int in_samplerate);

    int lame_set_num_channels(final Pointer lame, final int num_channels);

    int lame_init_params(final Pointer lame);

    int lame_encode_buffer_interleaved(final Pointer lame, final byte[] pcm, final int num_samples, final byte[] mp3buf, final int mp3buf_size);

    int lame_encode_flush(final Pointer lame, final byte[] mp3buf, final int mp3buf_size);

    int lame_close(final Pointer lame);

}
