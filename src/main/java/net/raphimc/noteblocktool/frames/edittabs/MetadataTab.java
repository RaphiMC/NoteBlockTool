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
package net.raphimc.noteblocktool.frames.edittabs;

import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.InvisiblePanel;
import net.raphimc.noteblocklib.format.mcsp2.model.McSp2Song;
import net.raphimc.noteblocklib.format.nbs.model.NbsSong;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.frames.ListFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MetadataTab extends EditTab {

    private final List<Runnable> saves = new ArrayList<>();
    private int gridy;

    public MetadataTab(final List<ListFrame.LoadedSong> songs) {
        super("Metadata", songs);
    }

    @Override
    protected void initComponents(JPanel center) {
        center.setLayout(new GridBagLayout());

        Song song = this.songs.get(0).song();
        if (song instanceof NbsSong nbsSong) {
            this.addString(center, "Title", () -> nbsSong.getTitleOr(""), nbsSong::setTitle);
            this.addString(center, "Author", () -> nbsSong.getAuthorOr(""), nbsSong::setAuthor);
            this.addString(center, "Original author", () -> nbsSong.getOriginalAuthorOr(""), nbsSong::setOriginalAuthor);
            this.addString(center, "Description", () -> nbsSong.getDescriptionOr(""), nbsSong::setDescription);
            this.addBoolean(center, "Auto Save", nbsSong::isAutoSave, nbsSong::setAutoSave);
            this.addNumber(center, "AutoSave Interval", nbsSong::getAutoSaveInterval, num -> nbsSong.setAutoSaveInterval(num.intValue()));
            this.addNumber(center, "Time Signature", nbsSong::getTimeSignature, num -> nbsSong.setTimeSignature(num.intValue()));
            this.addNumber(center, "Minutes Spent", nbsSong::getMinutesSpent, num -> nbsSong.setMinutesSpent(num.intValue()));
            this.addNumber(center, "Left Clicks", nbsSong::getLeftClicks, num -> nbsSong.setLeftClicks(num.intValue()));
            this.addNumber(center, "Right Clicks", nbsSong::getRightClicks, num -> nbsSong.setRightClicks(num.intValue()));
            this.addNumber(center, "Note Blocks Added", nbsSong::getNoteBlocksAdded, num -> nbsSong.setNoteBlocksAdded(num.intValue()));
            this.addNumber(center, "Note Blocks Removed", nbsSong::getNoteBlocksRemoved, num -> nbsSong.setNoteBlocksRemoved(num.intValue()));
            this.addString(center, "Source File Name", () -> nbsSong.getSourceFileNameOr(""), nbsSong::setSourceFileName);
            this.addBoolean(center, "Loop", nbsSong::isLoop, nbsSong::setLoop);
            this.addNumber(center, "Max Loop Count", nbsSong::getMaxLoopCount, num -> nbsSong.setMaxLoopCount(num.intValue()));
            this.addNumber(center, "Loop Start Tick", nbsSong::getLoopStartTick, num -> nbsSong.setLoopStartTick(num.shortValue()));
        } else if (song instanceof McSp2Song mcSp2Song) {
            this.addString(center, "Title", () -> mcSp2Song.getTitleOr(""), mcSp2Song::setTitle);
            this.addString(center, "Author", () -> mcSp2Song.getAuthorOr(""), mcSp2Song::setAuthor);
            this.addString(center, "Original author", () -> mcSp2Song.getOriginalAuthorOr(""), mcSp2Song::setOriginalAuthor);
            this.addNumber(center, "AutoSave Interval", mcSp2Song::getAutoSaveInterval, num -> mcSp2Song.setAutoSaveInterval(num.intValue()));
            this.addNumber(center, "Minutes Spent", mcSp2Song::getMinutesSpent, num -> mcSp2Song.setMinutesSpent(num.intValue()));
            this.addNumber(center, "Left Clicks", mcSp2Song::getLeftClicks, num -> mcSp2Song.setLeftClicks(num.intValue()));
            this.addNumber(center, "Right Clicks", mcSp2Song::getRightClicks, num -> mcSp2Song.setRightClicks(num.intValue()));
            this.addNumber(center, "Note Blocks Added", mcSp2Song::getNoteBlocksAdded, num -> mcSp2Song.setNoteBlocksAdded(num.intValue()));
            this.addNumber(center, "Note Blocks Removed", mcSp2Song::getNoteBlocksRemoved, num -> mcSp2Song.setNoteBlocksRemoved(num.intValue()));
        } else {
            GBC.create(center).grid(0, this.gridy).insets(5, 5, 0, 5).anchor(GBC.CENTER).add(new JLabel("No metadata available"));
        }
        GBC.create(center).grid(0, this.gridy++).insets(0).add(new InvisiblePanel(1, 5));
        GBC.fillVerticalSpace(center);
    }

    private void addBoolean(final JPanel parent, final String name, final Supplier<Boolean> getter, final Consumer<Boolean> setter) {
        GBC.create(parent).grid(0, this.gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel(name + ":"));
        GBC.create(parent).grid(1, this.gridy++).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JCheckBox("", getter.get()), checkBox -> {
            this.saves.add(() -> setter.accept(checkBox.isSelected()));
        });
    }

    private void addNumber(final JPanel parent, final String name, final Supplier<Number> getter, final Consumer<Number> setter) {
        GBC.create(parent).grid(0, this.gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel(name + ":"));
        GBC.create(parent).grid(1, this.gridy++).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new JSpinner(new SpinnerNumberModel(getter.get(), null, null, 1)), spinner -> {
            this.saves.add(() -> setter.accept((Number) spinner.getValue()));
        });
    }

    private void addString(final JPanel parent, final String name, final Supplier<String> getter, final Consumer<String> setter) {
        GBC.create(parent).grid(0, this.gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel(name + ":"));
        GBC.create(parent).grid(1, this.gridy++).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new JTextField(getter.get()), textField -> {
            this.saves.add(() -> setter.accept(textField.getText()));
        });
    }

    @Override
    public void apply(final Song song) {
        this.saves.forEach(Runnable::run);
    }

}
