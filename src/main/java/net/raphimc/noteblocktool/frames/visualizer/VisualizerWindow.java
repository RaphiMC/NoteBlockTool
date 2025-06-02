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
import net.raphimc.noteblocktool.util.SoundSystemSongPlayer;
import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.framebuffer.impl.TextureFramebuffer;
import net.raphimc.thingl.framebuffer.impl.WindowFramebuffer;
import net.raphimc.thingl.implementation.DebugMessageCallback;
import net.raphimc.thingl.implementation.GLFWWindowInterface;
import net.raphimc.thingl.wrapper.Blending;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

public class VisualizerWindow {

    private final DropRenderer dropRenderer;
    private final Runnable closeCallback;
    private final Thread renderThread;
    private long window;

    public VisualizerWindow(final SoundSystemSongPlayer songPlayer, final Runnable openCallback, final Runnable closeCallback) {
        this.dropRenderer = new DropRenderer(songPlayer);
        this.closeCallback = closeCallback;
        this.renderThread = new Thread(() -> {
            GLFWErrorCallback.createPrint(System.err).set();

            if (!GLFW.glfwInit()) {
                throw new IllegalStateException("Unable to initialize GLFW");
            }

            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
            GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 5);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

            this.window = GLFW.glfwCreateWindow(1280, 720, "NoteBlockTool Song Visualizer - " + songPlayer.getSong().getTitleOrFileNameOr("No Title"), 0L, 0L);
            if (this.window == 0L) {
                throw new RuntimeException("Failed to create the GLFW window");
            }
            openCallback.run();

            GLFW.glfwMakeContextCurrent(this.window);
            GLFW.glfwSwapInterval(1);
            GL.createCapabilities();

            ThinGL.LOGGER = SysoutLogger.builder().name("ThinGL").build();
            ThinGL.setInstance(new ExtendedThinGL(NoteBlockToolApplicationInterface::new, GLFWWindowInterface::new));
            DebugMessageCallback.install(false);

            this.dropRenderer.init();

            ThinGL.glStateManager().enable(GL11C.GL_BLEND);
            Blending.standardBlending();
            ThinGL.glStateManager().enable(GL11C.GL_DEPTH_TEST);
            ThinGL.glStateManager().setDepthFunc(GL11C.GL_LEQUAL);
            final TextureFramebuffer mainFramebuffer = new TextureFramebuffer();
            mainFramebuffer.setClearColor(0.5F, 0.5F, 0.5F, 0.5F);
            final Matrix4fStack positionMatrix = new Matrix4fStack(8);

            while (!GLFW.glfwWindowShouldClose(this.window)) {
                ThinGL.get().onStartFrame();
                mainFramebuffer.bind(true);
                mainFramebuffer.clear();

                positionMatrix.pushMatrix();
                this.dropRenderer.render(positionMatrix);
                positionMatrix.popMatrix();

                mainFramebuffer.unbind();
                mainFramebuffer.blitTo(WindowFramebuffer.INSTANCE, true, false, false);
                ThinGL.get().onFinishFrame();
                GLFW.glfwSwapBuffers(this.window);
                GLFW.glfwPollEvents();
                ThinGL.get().onEndFrame();
            }

            this.dropRenderer.free();
            ThinGL.get().free();
            GLFW.glfwDestroyWindow(this.window);
            GLFW.glfwTerminate();
            if (this.closeCallback != null) {
                this.closeCallback.run();
            }
        }, "Visualizer Render Thread");
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

}
