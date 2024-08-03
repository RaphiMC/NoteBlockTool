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
package net.raphimc.noteblocktool.util.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Ole32 extends Library {

    Ole32 INSTANCE = loadNative();

    int COINIT_MULTITHREADED = 0;

    static Ole32 loadNative() {
        try {
            return Native.load("Ole32", Ole32.class);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    long CoInitializeEx(final Pointer reserved, final int dwCoInit);

    void CoUninitialize();

}
