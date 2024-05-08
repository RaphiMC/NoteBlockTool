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
package net.raphimc.noteblocktool.util;

import net.raphimc.noteblocklib.format.nbs.NbsDefinitions;
import net.raphimc.noteblocklib.format.nbs.model.NbsNote;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.NoteWithPanning;
import net.raphimc.noteblocklib.model.NoteWithVolume;
import net.raphimc.noteblocklib.player.ISongPlayerCallback;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocklib.util.MinecraftDefinitions;

@FunctionalInterface
public interface DefaultSongPlayerCallback extends ISongPlayerCallback {

    @Override
    default void playNote(final Note note) {
        if (note.getInstrument() >= Instrument.values().length) return;
        final float volume;
        if (note instanceof NoteWithVolume) {
            final NoteWithVolume noteWithVolume = (NoteWithVolume) note;
            volume = noteWithVolume.getVolume();
        } else {
            volume = 100F;
        }
        if (volume <= 0) return;
        final float panning;
        if (note instanceof NoteWithPanning) {
            final NoteWithPanning noteWithPanning = (NoteWithPanning) note;
            panning = noteWithPanning.getPanning();
        } else {
            panning = 0F;
        }
        final float pitch;
        if (note instanceof NbsNote) {
            final NbsNote nbsNote = (NbsNote) note;
            pitch = MinecraftDefinitions.nbsPitchToMcPitch(NbsDefinitions.getPitch(nbsNote));
        } else {
            pitch = MinecraftDefinitions.mcKeyToMcPitch(MinecraftDefinitions.nbsKeyToMcKey(note.getKey()));
        }
        final Instrument instrument = Instrument.fromNbsId(note.getInstrument());
        final float playerVolume = volume / 100F;
        final float playerPanning = panning / 100F;

        this.playNote(instrument, playerVolume, pitch, playerPanning);
    }

    void playNote(final Instrument instrument, final float volume, final float pitch, final float panning);

}
