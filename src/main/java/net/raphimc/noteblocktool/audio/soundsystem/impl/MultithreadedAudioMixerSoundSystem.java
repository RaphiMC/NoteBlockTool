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

import net.raphimc.audiomixer.AudioMixer;
import net.raphimc.audiomixer.sound.source.MonoSound;
import net.raphimc.audiomixer.sound.source.StaticStereoSound;

import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class MultithreadedAudioMixerSoundSystem extends AudioMixerSoundSystem {

    private final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() - 4));
    private final AudioMixer[] audioMixers = new AudioMixer[this.threadPool.getCorePoolSize()];
    private final CyclicBarrier startBarrier = new CyclicBarrier(this.threadPool.getCorePoolSize() + 1);
    private final CyclicBarrier stopBarrier = new CyclicBarrier(this.threadPool.getCorePoolSize() + 1);
    private final int[][] threadSamples = new int[this.audioMixers.length][];
    private int currentMixer = 0;

    public MultithreadedAudioMixerSoundSystem(final Map<String, byte[]> soundData, final int maxSounds, final float playbackSpeed) {
        super(soundData, maxSounds, playbackSpeed);

        final int mixSampleCount = (int) (this.audioMixer.getAudioFormat().getSampleRate() / playbackSpeed) * this.audioMixer.getAudioFormat().getChannels();
        for (int i = 0; i < this.audioMixers.length; i++) {
            this.audioMixers[i] = new AudioMixer(this.audioMixer.getAudioFormat(), maxSounds / this.audioMixers.length);
        }
        for (int i = 0; i < this.threadPool.getCorePoolSize(); i++) {
            final int mixerIndex = i;
            this.threadPool.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        this.startBarrier.await();
                        this.threadSamples[mixerIndex] = this.audioMixers[mixerIndex].mix(mixSampleCount);
                        this.stopBarrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException ignored) {
                }
            });
        }
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.audioMixers[this.currentMixer].playSound(new MonoSound(this.sounds.get(sound), pitch, volume, panning));
        this.currentMixer = (this.currentMixer + 1) % this.audioMixers.length;
    }

    @Override
    public synchronized void postTick() {
        try {
            this.startBarrier.await();
            this.stopBarrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        for (int[] threadSamples : this.threadSamples) {
            this.audioMixer.playSound(new StaticStereoSound(threadSamples));
        }
        super.postTick();
        if (this.audioMixer.getActiveSounds() != 0) {
            throw new IllegalStateException("Mixer still has active sounds after mixing");
        }
    }

    @Override
    public synchronized void close() {
        this.threadPool.shutdownNow();
        super.close();
    }

    @Override
    public synchronized String getStatusLine() {
        int mixedSounds = 0;
        for (AudioMixer audioMixer : this.audioMixers) {
            mixedSounds += audioMixer.getMixedSounds();
        }
        return "Sounds: " + mixedSounds + " / " + this.maxSounds + ", " + this.threadPool.getActiveCount() + " threads";
    }

}