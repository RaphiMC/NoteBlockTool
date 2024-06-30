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
package net.raphimc.noteblocktool.frames.edittabs;

import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsNote;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.instruments.InstrumentsTable;
import net.raphimc.noteblocktool.frames.ListFrame;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomInstrumentsTab extends EditTab {

    private InstrumentsTable table;
    private Set<NbsCustomInstrument> usedInstruments;

    public CustomInstrumentsTab(final List<ListFrame.LoadedSong> songs) {
        super("Custom Instruments", songs);
    }

    @Override
    protected void initComponents(JPanel center) {
        this.removeAll();

        this.table = new InstrumentsTable(true);
        this.add(new FastScrollPane(this.table));
        this.usedInstruments = SongUtil.getUsedCustomInstruments(this.songs.get(0).getSong().getView());
        for (NbsCustomInstrument customInstrument : this.usedInstruments) {
            this.table.addRow(customInstrument.getName() + " (" + customInstrument.getSoundFileName() + ")", null);
        }
    }

    @Override
    public void apply(Song<?, ?, ?> song, SongView<?> view) {
        Map<NbsCustomInstrument, Instrument> replacements = new HashMap<>();
        int i = 0;
        for (NbsCustomInstrument customInstrument : this.usedInstruments) {
            Instrument replacement = (Instrument) this.table.getValueAt(i, 1);
            replacements.put(customInstrument, replacement);
            i++;
        }
        SongUtil.applyToAllNotes(view, note -> {
            if (note instanceof NbsNote) {
                final NbsCustomInstrument customInstrument = ((NbsNote) note).getCustomInstrument();
                Instrument replacement = replacements.get(customInstrument);
                if (replacement != null) {
                    note.setInstrument(replacement);
                }
            }
        });
    }

}
