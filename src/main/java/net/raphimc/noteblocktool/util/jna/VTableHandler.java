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

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public abstract class VTableHandler extends PointerType {

    public VTableHandler() {
    }

    public VTableHandler(final Pointer pvInstance) {
        super(pvInstance);
    }

    public Function getVtableFunction(final int index) {
        final Pointer vtblPtr = this.getPointer().getPointer(0);
        return Function.getFunction(vtblPtr.getPointer((long) index * Native.POINTER_SIZE));
    }

    public void setVtableFunction(final int index, final Pointer function) {
        final Pointer vtblPtr = this.getPointer().getPointer(0);
        vtblPtr.setPointer((long) index * Native.POINTER_SIZE, function);
    }

}
