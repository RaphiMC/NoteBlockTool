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
package net.raphimc.noteblocktool.util;

import net.raphimc.audiomixer.sound.Sound;
import net.raphimc.audiomixer.sound.impl.SubMixSound;

import javax.sound.sampled.AudioFormat;
import java.io.Closeable;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadedSubMixSound extends SubMixSound implements Closeable {

    private final ThreadPoolExecutor threadPool;
    private final CyclicBarrier startBarrier;
    private final CyclicBarrier stopBarrier;
    private final float[][] threadSamples;
    private final SubMixSound[] subMixSounds;
    private int currentSubMixSound = 0;
    private AudioFormat currentAudioFormat;
    private int currentRenderSampleCount;
    private long mixRenderTime;

    public ThreadedSubMixSound(final int threadCount) {
        this(threadCount, 512);
    }

    public ThreadedSubMixSound(final int threadCount, final int maxSounds) {
        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
        this.startBarrier = new CyclicBarrier(this.threadPool.getCorePoolSize() + 1);
        this.stopBarrier = new CyclicBarrier(this.threadPool.getCorePoolSize() + 1);
        this.threadSamples = new float[this.threadPool.getCorePoolSize()][];
        this.subMixSounds = new SubMixSound[this.threadPool.getCorePoolSize()];
        for (int i = 0; i < this.subMixSounds.length; i++) {
            this.subMixSounds[i] = new SubMixSound();
        }
        this.setMaxSounds(maxSounds);

        for (int i = 0; i < this.threadPool.getCorePoolSize(); i++) {
            final int mixerIndex = i;
            this.threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        this.startBarrier.await();
                        this.threadSamples[mixerIndex] = new float[this.currentRenderSampleCount];
                        this.subMixSounds[mixerIndex].render(this.currentAudioFormat, this.threadSamples[mixerIndex]);
                        this.stopBarrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException ignored) {
                }
            });
        }
    }

    @Override
    public void render(final AudioFormat audioFormat, final float[] finalMixBuffer) {
        final long start = System.nanoTime();
        this.currentAudioFormat = audioFormat;
        this.currentRenderSampleCount = finalMixBuffer.length;

        try {
            this.startBarrier.await();
            this.stopBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        for (float[] threadSamples : this.threadSamples) {
            for (int i = 0; i < finalMixBuffer.length; i++) {
                finalMixBuffer[i] += threadSamples[i];
            }
        }
        this.soundModifiers.modify(audioFormat, finalMixBuffer);

        this.mixRenderTime = System.nanoTime() - start;
    }

    @Override
    public synchronized void playSound(final Sound sound) {
        this.subMixSounds[this.currentSubMixSound].playSound(sound);
        this.currentSubMixSound = (this.currentSubMixSound + 1) % this.subMixSounds.length;
    }

    @Override
    public void stopSound(final Sound sound) {
        for (SubMixSound subMixSound : this.subMixSounds) {
            subMixSound.stopSound(sound);
        }
    }

    @Override
    public void stopAllSounds() {
        for (SubMixSound subMixSound : this.subMixSounds) {
            subMixSound.stopAllSounds();
        }
    }

    @Override
    public void close() {
        this.threadPool.shutdownNow();
    }

    @Override
    public int getMaxSounds() {
        int maxSounds = 0;
        for (SubMixSound subMixSound : this.subMixSounds) {
            maxSounds += subMixSound.getMaxSounds();
        }
        return maxSounds;
    }

    @Override
    public SubMixSound setMaxSounds(final int maxSounds) {
        if (this.subMixSounds == null) { // Called from super constructor
            return this;
        }

        for (SubMixSound subMixSound : this.subMixSounds) {
            subMixSound.setMaxSounds((int) Math.ceil((double) maxSounds / this.subMixSounds.length));
        }
        return this;
    }

    @Override
    public int getMixedSounds() {
        int mixedSounds = 0;
        for (SubMixSound subMixSound : this.subMixSounds) {
            mixedSounds += subMixSound.getMixedSounds();
        }
        return mixedSounds;
    }

    @Override
    public long getMixRenderTime() {
        return this.mixRenderTime;
    }

    @Override
    public int getActiveSounds() {
        int activeSounds = 0;
        for (SubMixSound subMixSound : this.subMixSounds) {
            activeSounds += subMixSound.getActiveSounds();
        }
        return activeSounds;
    }

    public int getThreadCount() {
        return this.threadPool.getCorePoolSize();
    }

}
