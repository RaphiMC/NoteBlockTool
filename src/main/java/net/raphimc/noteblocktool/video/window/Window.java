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
package net.raphimc.noteblocktool.video.window;

import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.implementation.application.ApplicationRunner;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class Window {

    private final Function<Window, Impl> implSupplier;
    private final ApplicationRunner.Configuration configuration = new ApplicationRunner.Configuration();
    private final List<Runnable> openListener = new ArrayList<>();
    private final List<Runnable> closeListener = new ArrayList<>();
    private Impl impl;

    public Window(final Function<Window, Impl> implSupplier) {
        this.implSupplier = implSupplier;
        this.addCloseListener(() -> this.impl = null);
    }

    public void open() {
        if (!this.isOpen()) {
            final Impl impl = this.implSupplier.apply(this);
            impl.runAsync();
            this.impl = impl;
        }
    }

    public void close() {
        if (this.isOpen()) {
            final Impl impl = this.impl;
            this.impl = null;
            impl.close();
        }
    }

    public boolean isOpen() {
        return this.impl != null;
    }

    public ApplicationRunner.Configuration getConfiguration() {
        return this.configuration;
    }

    public Impl getImpl() {
        return this.impl;
    }

    public void addOpenListener(final Runnable listener) {
        if (this.openListener.contains(listener)) {
            throw new RuntimeException("Open listener already registered");
        }
        this.openListener.add(listener);
    }

    public void removeOpenListener(final Runnable listener) {
        if (!this.openListener.remove(listener)) {
            throw new RuntimeException("Open listener not registered");
        }
    }

    public void addCloseListener(final Runnable listener) {
        if (this.closeListener.contains(listener)) {
            throw new RuntimeException("Close listener already registered");
        }
        this.closeListener.add(listener);
    }

    public void removeCloseListener(final Runnable listener) {
        if (!this.closeListener.remove(listener)) {
            throw new RuntimeException("Close listener not registered");
        }
    }

    public abstract void render(final Matrix4fStack positionMatrix);

    public abstract void freeRenderer();

    public void invokeOpenListener() {
        for (Runnable listener : this.openListener) {
            try {
                listener.run();
            } catch (Throwable e) {
                ThinGL.LOGGER.error("Exception while invoking window open listener", e);
            }
        }
    }

    public void invokeCloseListener() {
        for (Runnable listener : this.closeListener) {
            try {
                listener.run();
            } catch (Throwable e) {
                ThinGL.LOGGER.error("Exception while invoking window close listener", e);
            }
        }
    }

    public interface Impl {

        void runAsync();

        void close();

    }

}
