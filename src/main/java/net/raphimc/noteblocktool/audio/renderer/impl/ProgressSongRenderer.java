/*
 * This file is part of NoteBlockTool - https://github.com/RaphiMC/NoteBlockTool
 * Copyright (C) 2022-2026 RK_01/RaphiMC and contributors
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
package net.raphimc.noteblocktool.audio.renderer.impl;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.renderer.SongRenderer;

import java.util.List;

public class ProgressSongRenderer extends SongRenderer {

    private final FloatConsumer progressConsumer;
    private final int noteCount;
    private int processedNotes;

    public ProgressSongRenderer(final Song song, final int maxSounds, final boolean normalized, final boolean threaded, final PcmFloatAudioFormat audioFormat, final FloatConsumer progressConsumer) {
        super(song, maxSounds, normalized, threaded, audioFormat);
        this.noteCount = song.getNotes().getNoteCount();
        this.progressConsumer = progressConsumer;
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        super.playNotes(notes);
        this.processedNotes += notes.size();
        this.progressConsumer.accept(((float) this.processedNotes / this.noteCount) * 100F);
    }

}
