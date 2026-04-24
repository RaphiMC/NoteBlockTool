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
package net.raphimc.noteblocktool.elements.table.song;

import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.frames.ListFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class SongsTable extends JTable {

    public SongsTable() {
        super(new SongsTableModel("Path", "Title", "Author", "Length", "Notes", "Tempo", "Minecraft compatible"));

        this.getTableHeader().setReorderingAllowed(false);
        this.getColumnModel().getColumn(1).setPreferredWidth(250);
        this.getColumnModel().getColumn(3).setPreferredWidth(25);
        this.getColumnModel().getColumn(4).setPreferredWidth(25);
        this.getColumnModel().getColumn(5).setPreferredWidth(25);

        final TableRowSorter<TableModel> sorter = new TableRowSorter<>(this.getModel());
        final List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        for (int i = 0; i < this.getModel().getColumnCount(); i++) {
            sortKeys.add(new RowSorter.SortKey(i, SortOrder.UNSORTED));
        }
        sorter.setSortKeys(sortKeys);
        sorter.setComparator(4, Comparator.comparingInt(o -> (int) o));
        this.setRowSorter(sorter);
    }

    public void addRow(final ListFrame.LoadedSong song) {
        final DefaultTableModel model = (DefaultTableModel) this.getModel();
        model.addRow(new Object[]{
                song,
                song.song().getTitleOrFileNameOr("No Title"),
                song.song().getAuthorOr("Unknown"),
                song.song().getHumanReadableLength(),
                song.song().getNotes().getNoteCount(),
                song.song().getTempoEvents().getHumanReadableTempoRange(),
                this.isSchematicCompatible(song)
        });
    }

    public void refreshRow(final ListFrame.LoadedSong song) {
        final DefaultTableModel model = (DefaultTableModel) this.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0) == song) {
                model.setValueAt(song.song().getTitleOrFileNameOr("No Title"), i, 1);
                model.setValueAt(song.song().getAuthorOr("Unknown"), i, 2);
                model.setValueAt(song.song().getHumanReadableLength(), i, 3);
                model.setValueAt(song.song().getNotes().getNoteCount(), i, 4);
                model.setValueAt(song.song().getTempoEvents().getHumanReadableTempoRange(), i, 5);
                model.setValueAt(this.isSchematicCompatible(song), i, 6);
                break;
            }
        }
    }

    public void removeRowIf(final Predicate<ListFrame.LoadedSong> condition) {
        final DefaultTableModel model = (DefaultTableModel) this.getModel();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            if (condition.test((ListFrame.LoadedSong) model.getValueAt(i, 0))) {
                model.removeRow(i);
            }
        }
    }

    @Override
    public String getToolTipText(final MouseEvent event) {
        final int row = this.rowAtPoint(event.getPoint());
        final int column = this.columnAtPoint(event.getPoint());
        if (row < 0) {
            return null;
        } else if (column == 0) {
            return this.getValueAt(row, column).toString();
        } else if (column == 6) {
            return ((CompatibilityResult) this.getValueAt(row, column)).getTooltip();
        } else {
            return null;
        }
    }

    private CompatibilityResult isSchematicCompatible(final ListFrame.LoadedSong song) {
        final CompatibilityResult result = new CompatibilityResult();
        final float[] tempoRange = song.song().getTempoEvents().getTempoRange();
        if (tempoRange[0] != tempoRange[1] || (tempoRange[0] != 2.5F && tempoRange[0] != 5F && tempoRange[0] != 10F)) {
            result.add("The tempo must be 2.5, 5 or 10 TPS");
        }
        if (SongUtil.hasOutsideMinecraftOctaveRangeNotes(song.song())) {
            result.add("The song contains notes which are outside of the Minecraft octave range");
        }
        if (!SongUtil.getUsedNbsCustomInstruments(song.song()).isEmpty()) {
            result.add("The song contains notes with custom instruments");
        }
        return result;
    }

    private static class CompatibilityResult {

        private final List<String> reasons = new ArrayList<>();

        private void add(final String reason) {
            this.reasons.add(reason);
        }

        private String getTooltip() {
            if (!this.reasons.isEmpty()) {
                return String.join("\n", this.reasons);
            } else {
                return null;
            }
        }

        @Override
        public String toString() {
            return this.reasons.isEmpty() ? "Yes" : "No";
        }

    }

}
