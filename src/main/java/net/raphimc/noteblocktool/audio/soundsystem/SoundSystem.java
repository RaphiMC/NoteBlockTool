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
package net.raphimc.noteblocktool.audio.soundsystem;

public abstract class SoundSystem implements AutoCloseable {

    protected final int maxSounds;

    public SoundSystem(final int maxSounds) {
        this.maxSounds = maxSounds;
    }

    public abstract void playSound(final String sound, final float pitch, final float volume, final float panning);

    public void preTick() {
    }

    public void postTick() {
    }

    public abstract void stopSounds();

    @Override
    public abstract void close();

    public int getMaxSounds() {
        return this.maxSounds;
    }

    public abstract String getStatusLine();

    public abstract void setMasterVolume(final float volume);

}
