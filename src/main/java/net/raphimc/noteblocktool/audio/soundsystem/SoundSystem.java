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
package net.raphimc.noteblocktool.audio.soundsystem;

import net.raphimc.noteblocklib.util.Instrument;

public abstract class SoundSystem implements AutoCloseable {

    protected final int maxSounds;
    protected float masterVolume = 1F;

    public SoundSystem(final int maxSounds) {
        this.maxSounds = maxSounds;
    }

    public abstract void playNote(final Instrument instrument, final float volume, final float pitch, final float panning);

    public void writeSamples() {
    }

    public abstract void stopSounds();

    @Override
    public abstract void close();

    public int getMaxSounds() {
        return this.maxSounds;
    }

    public abstract int getSoundCount();

    public void setMasterVolume(final float volume) {
        this.masterVolume = volume;
    }

}
