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

import net.lenni0451.commons.color.Color;
import net.raphimc.noteblocktool.audio.player.AudioSystemSongPlayer;
import net.raphimc.thingl.implementation.application.GLFWApplicationRunner;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CancellationException;

public class VisualizerWindow extends GLFWApplicationRunner {

    private final DropRenderer dropRenderer;
    private final Runnable openCallback;
    private final Runnable closeCallback;

    public VisualizerWindow(final AudioSystemSongPlayer songPlayer, final Runnable openCallback, final Runnable closeCallback) {
        super(new Configuration()
                .setUseSeparateThreads(true)
                .setWindowTitle("NoteBlockTool Song Visualizer - " + songPlayer.getSong().getTitleOrFileNameOr("No Title"))
        );

        this.dropRenderer = new DropRenderer(songPlayer);
        this.openCallback = openCallback;
        this.closeCallback = closeCallback;

        this.launch();
        this.launchFuture.join();
    }

    public void close() {
        this.windowInterface.runOnWindowThread(() -> GLFW.glfwSetWindowShouldClose(this.window, true));
        try {
            this.freeFuture.join();
        } catch (CancellationException ignored) {
        }
    }

    @Override
    protected void setWindowHints() {
        super.setWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
    }

    @Override
    protected void createWindow() {
        super.createWindow();
        this.openCallback.run();
    }

    @Override
    protected void initThinGL() {
        this.thinGL = new ExtendedThinGL(this.windowInterface);
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
    protected void freeGL() {
        this.dropRenderer.free();
        super.freeGL();
    }

    @Override
    protected void freeWindowSystem() {
        super.freeWindowSystem();
        if (this.closeCallback != null) {
            this.closeCallback.run();
        }
    }

}
