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
package net.raphimc.noteblocktool.video.visualizer;

import net.raphimc.noteblocktool.video.window.Window;
import org.joml.Matrix4fStack;

import java.util.function.Function;
import java.util.function.Supplier;

public class VisualizerWindow extends Window {

    private Supplier<Visualizer> visualizerSupplier;
    private boolean visualizerSupplierChanged;
    private Visualizer visualizer;

    public VisualizerWindow(final Function<Window, Impl> implSupplier) {
        super(implSupplier);
    }

    @Override
    public void render(final Matrix4fStack positionMatrix) {
        if (this.visualizerSupplierChanged) {
            this.visualizerSupplierChanged = false;
            this.freeRenderer();
            if (this.visualizerSupplier != null) {
                this.visualizer = this.visualizerSupplier.get();
            }
        }
        if (this.visualizer != null) {
            this.visualizer.render(positionMatrix);
        }
    }

    @Override
    public void freeRenderer() {
        if (this.visualizer != null) {
            this.visualizer.free();
            this.visualizer = null;
        }
    }

    public void setVisualizer(final Supplier<Visualizer> visualizerSupplier) {
        this.visualizerSupplier = visualizerSupplier;
        this.visualizerSupplierChanged = true;
    }

}
