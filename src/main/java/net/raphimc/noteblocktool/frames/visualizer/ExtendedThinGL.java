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
package net.raphimc.noteblocktool.frames.visualizer;

import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.gl.renderer.impl.Renderer2D;
import net.raphimc.thingl.implementation.window.WindowInterface;

public class ExtendedThinGL extends ThinGL {

    public static ExtendedThinGL get() {
        return (ExtendedThinGL) ThinGL.get();
    }

    public static ExtendedRenderer2D renderer2D() {
        return get().getRenderer2D();
    }

    public ExtendedThinGL(final WindowInterface windowInterface) {
        super(windowInterface);
    }

    @Override
    public ExtendedRenderer2D getRenderer2D() {
        return (ExtendedRenderer2D) super.getRenderer2D();
    }

    @Override
    protected Renderer2D createRenderer2D() {
        return new ExtendedRenderer2D();
    }

}
