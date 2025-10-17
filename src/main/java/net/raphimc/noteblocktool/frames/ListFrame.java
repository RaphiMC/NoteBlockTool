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
package net.raphimc.noteblocktool.frames;

import net.lenni0451.commons.swing.GBC;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.TextOverlayPanel;
import net.raphimc.noteblocktool.elements.VerticalFileChooser;
import net.raphimc.noteblocktool.elements.drag.DragTable;
import net.raphimc.noteblocktool.elements.drag.DragTableDropTargetListener;
import net.raphimc.noteblocktool.elements.drag.DragTableModel;
import net.raphimc.noteblocktool.util.filefilter.NoteBlockFileFilter;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListFrame extends JFrame {

    private final List<LoadedSong> loadedSongs = new CopyOnWriteArrayList<>();
    private final DragTable table = new DragTable();
    private final JButton addButton = new JButton("Add");
    private final JButton removeButton = new JButton("Remove");
    private final JButton setCustomSoundsFolder = new JButton("Set Custom Sounds folder");
    private final JButton editButton = new JButton("Edit");
    private final JButton playButton = new JButton("Play");
    private final JButton exportButton = new JButton("Export");
    private DropTarget dropTarget;
    private TextOverlayPanel textOverlayPanel;

    public ListFrame() {
        this.setTitle("NoteBlockTool");
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 400);
        this.setLocationRelativeTo(null);

        this.initComponents();
        this.refreshButtons();

        this.setMinimumSize(this.getSize());
        this.setVisible(true);
    }

    private void initComponents() {
        final JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        this.setContentPane(root);

        root.add(new FastScrollPane(this.table), BorderLayout.CENTER);
        this.dropTarget = new DropTarget(this, new DragTableDropTargetListener(this, this::load));
        this.table.getSelectionModel().addListSelectionListener(e -> this.refreshButtons());
        this.table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) ListFrame.this.removeButton.doClick(0);
            }
        });
        this.addContextMenu();

        final JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        GBC.create(buttonPanel).gridx(0).insets(5, 5, 5, 0).anchor(GBC.LINE_START).add(this.addButton, () -> {
            this.addButton.addActionListener(e -> {
                VerticalFileChooser fileChooser = new VerticalFileChooser();
                fileChooser.setDialogTitle("Add Songs");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileFilter(new NoteBlockFileFilter());
                for (SongFormat songFormat : SongFormat.values()) fileChooser.addChoosableFileFilter(new NoteBlockFileFilter(songFormat));
                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    this.load(fileChooser.getSelectedFiles());
                }
            });
        });
        GBC.create(buttonPanel).gridx(1).insets(5, 5, 5, 0).anchor(GBC.LINE_START).add(this.removeButton, () -> {
            this.removeButton.addActionListener(e -> {
                final int[] selectedRows = this.table.getSelectedRows();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    final LoadedSong song = (LoadedSong) this.table.getValueAt(selectedRows[i], 0);
                    this.loadedSongs.remove(song);
                    ((DragTableModel) this.table.getModel()).removeRow(selectedRows[i]);
                }
            });
        });
        GBC.create(buttonPanel).gridx(2).insets(5, 5, 5, 0).anchor(GBC.LINE_START).add(this.setCustomSoundsFolder, () -> {
            this.setCustomSoundsFolder.addActionListener(e -> {
                VerticalFileChooser fileChooser = new VerticalFileChooser();
                fileChooser.setDialogTitle("Select Custom Sounds folder");
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        SoundMap.reload(fileChooser.getSelectedFile());
                    } catch (Throwable t) {
                        t.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Failed to load custom sounds:\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        });
        this.setCustomSoundsFolder.setToolTipText("Set the folder where sound files for custom instruments are located");
        GBC.create(buttonPanel).gridx(3).weightx(1).fill(GBC.HORIZONTAL).add(Box.createVerticalGlue());
        GBC.create(buttonPanel).gridx(4).insets(5, 5, 5, 0).anchor(GBC.LINE_START).add(this.editButton, () -> {
            this.editButton.addActionListener(e -> {
                final int[] rows = this.table.getSelectedRows();
                if (rows.length > 0) {
                    final List<LoadedSong> songs = new ArrayList<>();
                    for (int row : rows) songs.add((LoadedSong) this.table.getValueAt(row, 0));
                    new EditFrame(songs, this.table::refreshRow);
                }
            });
        });
        GBC.create(buttonPanel).gridx(5).insets(5, 5, 5, 0).anchor(GBC.LINE_START).add(this.playButton, () -> {
            this.playButton.addActionListener(e -> {
                final int[] rows = this.table.getSelectedRows();
                if (rows.length == 1) {
                    final LoadedSong song = (LoadedSong) this.table.getValueAt(rows[0], 0);
                    SongPlayerFrame.open(song.song());
                }
            });
        });
        GBC.create(buttonPanel).gridx(6).insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.exportButton, () -> {
            this.exportButton.addActionListener(e -> {
                SongPlayerFrame.close();
                this.setEnabled(false);
                final int[] rows = this.table.getSelectedRows();
                List<LoadedSong> songs = new ArrayList<>();
                for (int row : rows) songs.add((LoadedSong) this.table.getValueAt(row, 0));
                new ExportFrame(this, songs);
            });
        });
        root.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Point mousePosition = contextMenu.getInvoker().getMousePosition();
                if (mousePosition == null) return;

                int[] selectedRows = ListFrame.this.table.getSelectedRows();
                int hoveredRow = ListFrame.this.table.rowAtPoint(mousePosition);
                if (hoveredRow >= 0 && Arrays.stream(selectedRows).noneMatch(i -> i == hoveredRow)) {
                    ListFrame.this.table.setRowSelectionInterval(hoveredRow, hoveredRow);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        JMenuItem contextMenuRemove = new JMenuItem("Remove");
        contextMenuRemove.addActionListener(e -> this.removeButton.doClick(0));
        contextMenu.add(contextMenuRemove);
        JMenuItem contextMenuEdit = new JMenuItem("Edit");
        contextMenuEdit.addActionListener(e -> this.editButton.doClick(0));
        contextMenu.add(contextMenuEdit);
        JMenuItem contextMenuPlay = new JMenuItem("Play");
        contextMenuPlay.addActionListener(e -> this.playButton.doClick(0));
        contextMenu.add(contextMenuPlay);
        this.table.getSelectionModel().addListSelectionListener(e -> contextMenuPlay.setEnabled(this.table.getSelectedRows().length == 1));
        JMenuItem contextMenuExport = new JMenuItem("Export");
        contextMenuExport.addActionListener(e -> this.exportButton.doClick(0));
        contextMenu.add(contextMenuExport);
        this.table.setComponentPopupMenu(contextMenu);
    }

    private void refreshButtons() {
        this.removeButton.setEnabled(this.table.getSelectedRows().length > 0);
        this.editButton.setEnabled(this.table.getSelectedRows().length > 0);
        this.playButton.setEnabled(this.table.getSelectedRows().length == 1);
        this.exportButton.setEnabled(this.table.getSelectedRows().length > 0);
    }

    private void load(final File... files) {
        this.runSync(() -> {
            this.setDropTarget(null);
            this.textOverlayPanel = new TextOverlayPanel("Loading Songs...");
            this.setGlassPane(this.textOverlayPanel);
            this.textOverlayPanel.setVisible(true);
        });
        final Map<File, Throwable> failedFiles = new HashMap<>();
        CompletableFuture.runAsync(() -> {
            final Queue<File> queue = new ArrayDeque<>(Arrays.asList(files));
            while (!queue.isEmpty()) {
                final File file = queue.poll();
                if (file.isDirectory()) {
                    final File[] subFiles = file.listFiles();
                    if (subFiles != null) queue.addAll(Arrays.asList(subFiles));
                } else if (file.isFile()) {
                    if (this.loadedSongs.stream().anyMatch(s -> s.file().equals(file))) continue;
                    try {
                        final Song song = NoteBlockLib.readSong(file);
                        final LoadedSong loadedSong = new LoadedSong(file, song);
                        this.loadedSongs.add(loadedSong);
                        this.runSync(() -> {
                            this.table.addRow(loadedSong);
                            StringBuilder text = new StringBuilder("Loading Songs (" + this.loadedSongs.size() + ")...\n");
                            for (int i = 0; i < 5; i++) {
                                int index = this.loadedSongs.size() - i - 1;
                                if (index < 0) break;
                                text.append(this.loadedSongs.get(index).file().getName()).append("\n");
                            }
                            if (text.toString().endsWith("\n")) text = new StringBuilder(text.substring(0, text.length() - 1));
                            this.textOverlayPanel.setText(text.toString());
                        });
                    } catch (Throwable t) {
                        t.printStackTrace();
                        failedFiles.put(file, t);
                    }
                }
            }
        }).thenAcceptAsync(v -> {
            this.runSync(() -> {
                this.textOverlayPanel.setVisible(false);
                this.setGlassPane(new JPanel());
                this.setDropTarget(this.dropTarget);
            });
            if (!failedFiles.isEmpty()) {
                String message;
                if (failedFiles.size() == 1) {
                    Map.Entry<File, Throwable> entry = failedFiles.entrySet().iterator().next();
                    message = "Failed to load song:\n" + entry.getKey().getAbsolutePath() + "\n" + entry.getValue().getMessage();
                } else {
                    message = "Failed to load " + failedFiles.size() + " songs";
                }
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void runSync(final Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Throwable t) {
                throw new RuntimeException("Failed to run task", t);
            }
        }
    }

    public record LoadedSong(File file, Song song) {

        @Override
        public String toString() {
            return this.file.getAbsolutePath();
        }

    }

}
