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
package net.raphimc.noteblocktool.frames.visualizer;

import net.lenni0451.commons.color.Color;
import net.raphimc.noteblocktool.util.SoundSystemSongPlayer;
import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.implementation.application.StandaloneApplicationRunner;
import net.raphimc.thingl.implementation.window.GLFWWindowInterface;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;

public class VisualizerWindow extends StandaloneApplicationRunner {

    private final DropRenderer dropRenderer;
    private final Runnable openCallback;
    private final Runnable closeCallback;
    private final Thread renderThread;

    public VisualizerWindow(final SoundSystemSongPlayer songPlayer, final Runnable openCallback, final Runnable closeCallback) {
        super(new Configuration()
                .setWindowTitle("NoteBlockTool Song Visualizer - " + songPlayer.getSong().getTitleOrFileNameOr("No Title")));

        this.dropRenderer = new DropRenderer(songPlayer);
        this.openCallback = openCallback;
        this.closeCallback = closeCallback;
        this.renderThread = new Thread(this::launch, "Visualizer Render Thread");
        this.renderThread.setDaemon(true);
        this.renderThread.start();

        while (!ThinGL.isInitialized() && this.renderThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public boolean isRenderThreadAlive() {
        return this.renderThread.isAlive() && !this.renderThread.isInterrupted();
    }

    public void close() {
        if (!this.isRenderThreadAlive()) {
            return;
        }

        ThinGL.get().runOnRenderThread(() -> GLFW.glfwSetWindowShouldClose(this.window, true));
        try {
            this.renderThread.join(1000);
        } catch (InterruptedException ignored) {
        }
        if (this.renderThread.isAlive()) {
            this.renderThread.interrupt();
        }
    }

    @Override
    protected void setWindowFlags() {
        super.setWindowFlags();
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
    }

    @Override
    protected void createWindow() {
        super.createWindow();
        this.openCallback.run();
    }

    @Override
    protected ThinGL createThinGL() {
        return new ExtendedThinGL(new GLFWWindowInterface());
    }

    @Override
    protected void init() {
        super.init();
        this.dropRenderer.init();
        this.mainFramebuffer.setClearColor(Color.GRAY);
    }

    @Override
    protected void render(final Matrix4fStack positionMatrix) {
        this.dropRenderer.render(positionMatrix);
    }

    @Override
    protected void free() {
        this.dropRenderer.free();
        super.free();
        if (this.closeCallback != null) {
            this.closeCallback.run();
        }
    }

}
