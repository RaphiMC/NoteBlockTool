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
package net.raphimc.noteblocktool.audio.soundsystem.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MultithreadedJavaxSoundSystem extends JavaxSoundSystem {

    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
    private final BlockingQueue<SoundInstance> soundsToRender = new ArrayBlockingQueue<>(8192);
    private final BlockingQueue<int[]> renderResults = new ArrayBlockingQueue<>(8192);

    public MultithreadedJavaxSoundSystem(final int maxSounds, final float playbackSpeed) {
        super(maxSounds, playbackSpeed);

        for (int i = 0; i < this.threadPool.getCorePoolSize(); i++) {
            this.threadPool.submit(() -> {
                try {
                    while (true) {
                        this.renderResults.put(this.soundsToRender.take().render());
                    }
                } catch (InterruptedException ignored) {
                }
            });
        }
    }

    @Override
    public void writeSamples() {
        final long[] samples = new long[this.samplesPerTick];
        for (SoundInstance playingSound : this.playingSounds) {
            this.soundsToRender.offer(playingSound);
        }

        while (this.renderResults.size() != this.playingSounds.size() && !Thread.currentThread().isInterrupted()) {
            // Wait for all sounds to be rendered
        }

        while (!this.renderResults.isEmpty()) {
            final int[] result = this.renderResults.poll();
            for (int i = 0; i < samples.length; i++) {
                samples[i] += result[i];
            }
        }

        this.dataLine.write(this.write(samples), 0, samples.length * 2);

        this.playingSounds.removeIf(SoundInstance::isFinished);
    }

    @Override
    public void close() {
        this.threadPool.shutdownNow();
        super.close();
    }

    @Override
    public String getStatusLine() {
        return super.getStatusLine() + ", " + this.threadPool.getActiveCount() + " threads";
    }

}
