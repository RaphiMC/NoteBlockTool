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
package net.raphimc.noteblocktool.audio.soundsystem.impl;

import net.raphimc.audiomixer.pcmsource.impl.MonoIntPcmSource;
import net.raphimc.audiomixer.sound.impl.pcm.OptimizedMonoSound;
import net.raphimc.noteblocktool.util.ThreadedSubMixSound;

import java.util.Map;

public class MultithreadedAudioMixerSoundSystem extends AudioMixerSoundSystem {

    private final ThreadedSubMixSound threadedSubMixSound;

    public MultithreadedAudioMixerSoundSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        super(soundData, 1);
        this.threadedSubMixSound = new ThreadedSubMixSound(Math.max(2, Runtime.getRuntime().availableProcessors() - 4), maxSounds);
        this.audioMixer.playSound(this.threadedSubMixSound);
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.threadedSubMixSound.playSound(new OptimizedMonoSound(new MonoIntPcmSource(this.sounds.get(sound)), pitch, volume, panning));
    }

    @Override
    public synchronized void stopSounds() {
        super.stopSounds();
        this.threadedSubMixSound.stopAllSounds();
        this.audioMixer.playSound(this.threadedSubMixSound);
    }

    @Override
    public synchronized void close() {
        super.close();
        this.threadedSubMixSound.close();
    }

    @Override
    public int getMaxSounds() {
        return this.threadedSubMixSound.getMaxSounds();
    }

    @Override
    public String getStatusLine() {
        return "Sounds: " + this.threadedSubMixSound.getMixedSounds() + " / " + this.threadedSubMixSound.getMaxSounds() + ", " + this.threadedSubMixSound.getThreadCount() + " threads";
    }

}
