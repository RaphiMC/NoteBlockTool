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
package net.raphimc.noteblocktool.elements.drag;

import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.frames.ListFrame;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DragTable extends JTable {

    public DragTable() {
        super(new DragTableModel("Path", "Title", "Author", "Length", "Notes", "Tempo", "Minecraft compatible"));

        this.getTableHeader().setReorderingAllowed(false);
        this.getColumnModel().getColumn(1).setPreferredWidth(250);
        this.getColumnModel().getColumn(3).setPreferredWidth(25);
        this.getColumnModel().getColumn(4).setPreferredWidth(25);
        this.getColumnModel().getColumn(5).setPreferredWidth(25);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(this.getModel());
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();
        for (int i = 0; i < 6; i++) sortKeys.add(new RowSorter.SortKey(i, SortOrder.UNSORTED));
        sorter.setSortKeys(sortKeys);
        sorter.setComparator(4, Comparator.comparingInt(o -> (int) o));
        this.setRowSorter(sorter);
    }

    public void addRow(final ListFrame.LoadedSong song) {
        ((DragTableModel) this.getModel()).addRow(new Object[]{
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
        for (int i = 0; i < this.getModel().getRowCount(); i++) {
            if (this.getModel().getValueAt(i, 0) == song) {
                this.getModel().setValueAt(song.song().getTitleOrFileNameOr("No Title"), i, 1);
                this.getModel().setValueAt(song.song().getAuthorOr("Unknown"), i, 2);
                this.getModel().setValueAt(song.song().getHumanReadableLength(), i, 3);
                this.getModel().setValueAt(song.song().getNotes().getNoteCount(), i, 4);
                this.getModel().setValueAt(song.song().getTempoEvents().getHumanReadableTempoRange(), i, 5);
                this.getModel().setValueAt(this.isSchematicCompatible(song), i, 6);
                break;
            }
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int row = this.rowAtPoint(event.getPoint());
        int column = this.columnAtPoint(event.getPoint());
        if (row < 0) return null;
        if (column == 0) return this.getModel().getValueAt(row, column).toString();
        if (column == 6) {
            CompatibilityResult result = (CompatibilityResult) this.getModel().getValueAt(row, column);
            return result.getTooltip();
        }
        return null;
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
        private final List<String> reasons;

        private CompatibilityResult() {
            this.reasons = new ArrayList<>();
        }

        private void add(final String reason) {
            this.reasons.add(reason);
        }

        private String getTooltip() {
            if (this.reasons.isEmpty()) return null;
            return String.join("\n", this.reasons);
        }

        @Override
        public String toString() {
            return this.reasons.isEmpty() ? "Yes" : "No";
        }
    }

}
