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
package net.raphimc.noteblocktool.audio.system;

import net.raphimc.audiomixer.util.PcmFloatAudioFormat;

import java.util.Map;

public abstract class AudioSystem implements AutoCloseable {

    private final int maxSounds;
    private final PcmFloatAudioFormat loopbackAudioFormat;

    public AudioSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        this.maxSounds = maxSounds;
        this.loopbackAudioFormat = null;
    }

    public AudioSystem(final Map<String, byte[]> soundData, final int maxSounds, final PcmFloatAudioFormat loopbackAudioFormat) {
        if (loopbackAudioFormat == null) {
            throw new IllegalArgumentException("Loopback audio format cannot be null");
        }
        this.maxSounds = maxSounds;
        this.loopbackAudioFormat = loopbackAudioFormat;
    }

    public void preTick() {
    }

    public abstract void playSound(final String sound, final float pitch, final float volume, final float panning);

    public abstract void stopAllSounds();

    public abstract float[] render(final int frameCount);

    @Override
    public abstract void close();

    public abstract void setMasterVolume(final float volume);

    public abstract Integer getPlayingSounds();

    public int getMaxSounds() {
        return this.maxSounds;
    }

    public abstract Float getCpuLoad();

    public PcmFloatAudioFormat getLoopbackAudioFormat() {
        return this.loopbackAudioFormat;
    }

}
