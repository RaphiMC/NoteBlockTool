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
package net.raphimc.noteblocktool.audio.util;

public class LameException extends RuntimeException {

    public static int check(final int result, final String message) {
        if (result < 0) {
            throw new LameException(result, message);
        }
        return result;
    }

    private final int errorCode;

    public LameException(final int errorCode, final String message) {
        super(message + " (LAME error code: " + errorCode + ")");
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }

}
