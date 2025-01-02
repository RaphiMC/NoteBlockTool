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

import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.audio.soundsystem.impl.AudioMixerSoundSystem;

import java.io.File;
import java.util.List;

public class SoundSystemSongPlayer extends SongPlayer {

    private SoundSystem soundSystem;
    private long availableNanosPerTick;
    private long neededNanosPerTick;

    public SoundSystemSongPlayer(final Song song) {
        super(song);
    }

    public void start(final SoundSystem soundSystem) {
        this.soundSystem = soundSystem;
        super.start();
    }

    @Override
    public void start() {
        throw new UnsupportedOperationException("Use start(SoundSystem) instead");
    }

    @Override
    protected void createTickTask() {
        super.createTickTask();
        if (this.soundSystem instanceof AudioMixerSoundSystem audioMixerSoundSystem) {
            audioMixerSoundSystem.updateMixSliceSize(this.getCurrentTicksPerSecond());
        }
    }

    @Override
    protected void tick() {
        this.availableNanosPerTick = (long) (1_000_000_000L / this.getCurrentTicksPerSecond());
        final long start = System.nanoTime();
        super.tick();
        this.neededNanosPerTick = System.nanoTime() - start;
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        for (Note note : notes) {
            if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                this.soundSystem.playSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), note.getPitch(), note.getVolume(), note.getPanning());
            } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                this.soundSystem.playSound(instrument.getSoundFilePathOr("").replace(File.separatorChar, '/'), note.getPitch(), note.getVolume(), note.getPanning());
            } else {
                throw new IllegalArgumentException("Unsupported instrument type: " + note.getInstrument().getClass().getName());
            }
        }
    }

    @Override
    protected boolean preTick() {
        this.soundSystem.preTick();
        return super.preTick();
    }

    @Override
    protected void postTick() {
        if (this.soundSystem instanceof AudioMixerSoundSystem audioMixerSoundSystem) {
            audioMixerSoundSystem.mixSlice();
        }
    }

    public float getCpuLoad() {
        return (float) this.neededNanosPerTick / this.availableNanosPerTick;
    }

}
