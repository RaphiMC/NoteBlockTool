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
import net.raphimc.thingl.gl.resource.framebuffer.Framebuffer;
import net.raphimc.thingl.implementation.application.ApplicationRunner;
import net.raphimc.thingl.implementation.application.AwtApplicationRunner;
import net.raphimc.thingl.implementation.application.GLFWApplicationRunner;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Platform;

public class VisualizerWindow {

    private final AudioSystemSongPlayer songPlayer;
    private final Runnable openCallback;
    private final Runnable closeCallback;
    private final ApplicationRunner window;
    private DropRenderer dropRenderer;

    public VisualizerWindow(final AudioSystemSongPlayer songPlayer, final Runnable openCallback, final Runnable closeCallback) {
        this.songPlayer = songPlayer;
        this.openCallback = openCallback;
        this.closeCallback = closeCallback;

        final ApplicationRunner.Configuration configuration = new ApplicationRunner.Configuration()
                .setWindowTitle("NoteBlockTool Song Visualizer - " + songPlayer.getSong().getTitleOrFileNameOr("No Title"));
        if (Platform.get() != Platform.MACOSX) {
            this.window = new GLFWWindow(configuration);
        } else {
            this.window = new AwtWindow(configuration);
        }
    }

    public void close() {
        this.window.close();
    }

    private void createWindow() {
        this.openCallback.run();
    }

    private void init(final Framebuffer mainFramebuffer) {
        this.dropRenderer = new DropRenderer(this.songPlayer);
        mainFramebuffer.setClearColor(Color.GRAY);
    }

    private void render(final Matrix4fStack positionMatrix) {
        this.dropRenderer.render(positionMatrix);
    }

    private void freeGL() {
        this.dropRenderer.free();
        this.dropRenderer = null;
    }

    private void freeWindowSystem() {
        if (this.closeCallback != null) {
            this.closeCallback.run();
        }
    }

    private class GLFWWindow extends GLFWApplicationRunner {

        public GLFWWindow(final Configuration configuration) {
            super(configuration);
            new Thread(this, configuration.getWindowTitle() + " Thread").start();
            this.launchFuture.join();
        }

        @Override
        protected void setWindowHints() {
            super.setWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        }

        @Override
        protected void createWindow() {
            super.createWindow();
            VisualizerWindow.this.createWindow();
        }

        @Override
        protected void initThinGL() {
            this.thinGL = new ExtendedThinGL(this.windowInterface);
        }

        @Override
        protected void init() {
            super.init();
            VisualizerWindow.this.init(this.mainFramebuffer);
        }

        @Override
        protected void render(final Matrix4fStack positionMatrix) {
            VisualizerWindow.this.render(positionMatrix);
        }

        @Override
        protected void freeGL() {
            VisualizerWindow.this.freeGL();
            super.freeGL();
        }

        @Override
        protected void freeWindowSystem() {
            super.freeWindowSystem();
            VisualizerWindow.this.freeWindowSystem();
        }

    }

    private class AwtWindow extends AwtApplicationRunner {

        public AwtWindow(final Configuration configuration) {
            super(configuration);
            new Thread(this, configuration.getWindowTitle() + " Thread").start();
            this.launchFuture.join();
        }

        @Override
        protected void createWindow() {
            super.createWindow();
            this.frame.setAutoRequestFocus(false);
            VisualizerWindow.this.createWindow();
        }

        @Override
        protected void initThinGL() {
            this.thinGL = new ExtendedThinGL(this.windowInterface);
        }

        @Override
        protected void init() {
            super.init();
            VisualizerWindow.this.init(this.mainFramebuffer);
        }

        @Override
        protected void render(final Matrix4fStack positionMatrix) {
            VisualizerWindow.this.render(positionMatrix);
        }

        @Override
        protected void freeGL() {
            VisualizerWindow.this.freeGL();
            super.freeGL();
        }

        @Override
        protected void freeWindowSystem() {
            super.freeWindowSystem();
            VisualizerWindow.this.freeWindowSystem();
        }

    }

}
