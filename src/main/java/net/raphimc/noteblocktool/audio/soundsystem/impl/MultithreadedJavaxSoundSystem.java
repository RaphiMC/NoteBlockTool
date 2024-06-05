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

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class MultithreadedJavaxSoundSystem extends JavaxSoundSystem {

    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 4));
    private final Queue<SoundInstance> soundsToRender = new ConcurrentLinkedQueue<>();
    private final Queue<SoundInstance> soundsToMerge = new ConcurrentLinkedQueue<>();
    private final AtomicInteger syncLock = new AtomicInteger(0);
    private final long[][] threadSamples;

    public MultithreadedJavaxSoundSystem(final int maxSounds, final float playbackSpeed) {
        super(maxSounds, playbackSpeed);

        final int mergingThreads = Math.max(1, this.threadPool.getCorePoolSize() / 3);
        final int renderingThreads = this.threadPool.getCorePoolSize() - mergingThreads;
        this.threadSamples = new long[mergingThreads][];
        for (int i = 0; i < mergingThreads; i++) {
            this.threadSamples[i] = new long[this.samplesPerTick];
        }

        for (int i = 0; i < renderingThreads; i++) {
            this.threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        final SoundInstance soundInstance = this.soundsToRender.poll();
                        if (soundInstance == null) {
                            Thread.sleep(1);
                            continue;
                        }
                        soundInstance.render();
                        this.soundsToMerge.add(soundInstance);
                    }
                } catch (InterruptedException ignored) {
                }
            });
        }
        for (int i = 0; i < mergingThreads; i++) {
            final int finalI = i;
            this.threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        final SoundInstance soundInstance = this.soundsToMerge.poll();
                        if (soundInstance == null) {
                            Thread.sleep(1);
                            continue;
                        }
                        soundInstance.write(this.threadSamples[finalI]);
                        this.syncLock.decrementAndGet();
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
            this.soundsToRender.add(playingSound);
            this.syncLock.incrementAndGet();
        }

        while (this.syncLock.get() != 0 && !Thread.currentThread().isInterrupted()) {
            // Wait for all sounds to be rendered and merged
        }

        for (long[] threadSamples : this.threadSamples) {
            for (int i = 0; i < samples.length; i++) {
                samples[i] += threadSamples[i];
            }
            Arrays.fill(threadSamples, 0);
        }
        this.dataLine.write(this.writeNormalized(samples), 0, samples.length * 2);
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
