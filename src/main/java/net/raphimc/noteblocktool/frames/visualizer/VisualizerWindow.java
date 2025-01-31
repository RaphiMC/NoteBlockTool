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

import net.lenni0451.commons.logging.impl.SysoutLogger;
import net.raphimc.noteblocktool.frames.SongPlayerFrame;
import net.raphimc.noteblocktool.util.SoundSystemSongPlayer;
import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.framebuffer.impl.TextureFramebuffer;
import net.raphimc.thingl.framebuffer.impl.WindowFramebuffer;
import net.raphimc.thingl.implementation.DebugMessageCallback;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL14C;

public class VisualizerWindow {

    private static VisualizerWindow INSTANCE;

    private final Thread renderThread;
    private long window;

    private DropRenderer dropRenderer;

    public static VisualizerWindow getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VisualizerWindow();
        }
        return INSTANCE;
    }

    private VisualizerWindow() {
        this.renderThread = new Thread(() -> {
            GLFWErrorCallback.createPrint(System.err).set();

            if (!GLFW.glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, 1);

            this.window = GLFW.glfwCreateWindow(1280, 720, "NoteBlockTool Song Visualizer", 0L, 0L);
            if (this.window == 0L) {
                throw new RuntimeException("Failed to create the GLFW window");
            }

            GLFW.glfwMakeContextCurrent(this.window);
            GLFW.glfwSwapInterval(1);
            GL.createCapabilities();

            GLFW.glfwSetWindowCloseCallback(this.window, window -> SongPlayerFrame.close());

            ThinGL.LOGGER = SysoutLogger.builder().name("ThinGL").build();
            ThinGL.init(new NoteBlockToolThinGLImplementation());
            DebugMessageCallback.install(false);

            GL11C.glEnable(GL11C.GL_BLEND);
            GL14C.glBlendFuncSeparate(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA, GL11C.GL_ONE, GL11C.GL_ZERO);
            GL11C.glEnable(GL11C.GL_DEPTH_TEST);
            GL11C.glDepthFunc(GL11C.GL_LEQUAL);
            final TextureFramebuffer mainFramebuffer = new TextureFramebuffer();
            final Matrix4fStack positionMatrix = new Matrix4fStack(8);

            while (/*!GLFW.glfwWindowShouldClose(this.window)*/true) {
                mainFramebuffer.bind(true);
                mainFramebuffer.clear();

                positionMatrix.pushMatrix();
                if (this.dropRenderer != null) {
                    this.dropRenderer.render(positionMatrix);
                }
                positionMatrix.popMatrix();

                mainFramebuffer.blitTo(WindowFramebuffer.INSTANCE, true, false, false);
                ThinGL.endFrame();
                GLFW.glfwSwapBuffers(this.window);
                GLFW.glfwPollEvents();
            }

            // GLFW.glfwDestroyWindow(this.window);
            // GLFW.glfwTerminate();
        }, "Visualizer Render Thread");
        this.renderThread.setDaemon(true);
        this.renderThread.start();

        while (ThinGL.getImplementation() == null && this.renderThread.isAlive()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void open(final SoundSystemSongPlayer songPlayer) {
        final DropRenderer dropRenderer = new DropRenderer(songPlayer);
        ThinGL.runOnRenderThread(() -> {
            dropRenderer.init();
            GLFW.glfwSetWindowTitle(this.window, "NoteBlockTool Song Visualizer - " + songPlayer.getSong().getTitleOrFileNameOr("No Title"));
            GLFW.glfwShowWindow(this.window);
            this.dropRenderer = dropRenderer;
        });
    }

    public void hide() {
        ThinGL.runOnRenderThread(() -> {
            GLFW.glfwHideWindow(this.window);
            this.dropRenderer.delete();
            this.dropRenderer = null;
        });
    }

    public void close() {
        if (!this.renderThread.isAlive() || this.renderThread.isInterrupted()) {
            return;
        }

        ThinGL.runOnRenderThread(() -> GLFW.glfwSetWindowShouldClose(GLFW.glfwGetCurrentContext(), true));
        try {
            this.renderThread.join(1000);
        } catch (InterruptedException ignored) {
        }
        if (this.renderThread.isAlive()) {
            this.renderThread.interrupt();
        }
    }

}
