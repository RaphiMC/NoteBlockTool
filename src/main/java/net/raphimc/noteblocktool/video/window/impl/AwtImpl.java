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
package net.raphimc.noteblocktool.video.window.impl;

import net.raphimc.noteblocktool.video.thingl.ExtendedThinGL;
import net.raphimc.noteblocktool.video.window.Window;
import net.raphimc.thingl.implementation.application.AwtApplicationRunner;
import org.joml.Matrix4fStack;

public class AwtImpl extends AwtApplicationRunner implements Window.Impl {

    private final Window window;

    public AwtImpl(final Window window) {
        super(window.getConfiguration());
        this.window = window;
    }

    @Override
    protected void launchWindowSystem() {
        super.launchWindowSystem();
        this.window.invokeOpenListener();
    }

    @Override
    protected void initThinGL() {
        this.thinGL = new ExtendedThinGL(this.windowInterface);
    }

    @Override
    protected void render(final Matrix4fStack positionMatrix) {
        this.window.render(positionMatrix);
    }

    @Override
    protected void freeGL() {
        this.window.freeRenderer();
        super.freeGL();
    }

    @Override
    protected void freeWindowSystem() {
        super.freeWindowSystem();
        this.window.invokeCloseListener();
    }

}
