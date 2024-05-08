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
package net.raphimc.noteblocktool.frames;

import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.lenni0451.commons.swing.layouts.VerticalLayout;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.mcsp.McSpSong;
import net.raphimc.noteblocklib.format.mcsp.model.McSpHeader;
import net.raphimc.noteblocklib.format.nbs.NbsSong;
import net.raphimc.noteblocklib.format.nbs.model.NbsHeader;
import net.raphimc.noteblocklib.format.nbs.model.NbsNote;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.util.SongResampler;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.audio.export.impl.JavaxAudioExporter;
import net.raphimc.noteblocktool.audio.export.impl.OpenALAudioExporter;
import net.raphimc.noteblocktool.audio.soundsystem.OpenALSoundSystem;
import net.raphimc.noteblocktool.util.filefilter.SingleFileFilter;

import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Consumer;

public class ExportFrame extends JFrame {

    private final ListFrame parent;
    private final List<ListFrame.LoadedSong> loadedSongs;
    private final JComboBox<String> format = new JComboBox<>(new String[]{"NBS", "WAV", "AIF"});
    private final JLabel soundSystemLabel = new JLabel("Sound System:");
    private final JComboBox<String> soundSystem = new JComboBox<>(new String[]{"OpenAL (better sound quality)", "Javax (faster, normalized, multithreaded, mono only)"});
    private final JLabel sampleRateLabel = new JLabel("Sample Rate:");
    private final JSpinner sampleRate = new JSpinner(new SpinnerNumberModel(44_100, 8_000, 192_000, 1_000));
    private final JLabel bitDepthLabel = new JLabel("Bit Depth:");
    private final JComboBox<String> bitDepth = new JComboBox<>(new String[]{"8", "16", "32"});
    private final JLabel channelsLabel = new JLabel("Channels:");
    private final JComboBox<String> channels = new JComboBox<>(new String[]{"Mono", "Stereo"});
    private JPanel progressPanel;
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton exportButton = new JButton("Export");
    private Thread exportThread;

    public ExportFrame(final ListFrame parent, final List<ListFrame.LoadedSong> loadedSongs) {
        this.parent = parent;
        this.loadedSongs = loadedSongs;

        this.setTitle("Export (" + loadedSongs.size() + " song" + (loadedSongs.size() == 1 ? "" : "s") + ")");
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setSize(500, 400);
        this.setLocationRelativeTo(null);

        this.initComponents();
        this.updateVisibility();
        this.initFrameHandler();

        this.setMinimumSize(this.getSize());
        this.setVisible(true);
    }

    private void initComponents() {
        JPanel root = new JPanel();
        root.setLayout(new GridBagLayout());
        this.setContentPane(root);
        int gridy = 0;

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Format:"));
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.format, () -> {
            this.format.addActionListener(e -> this.updateVisibility());
        });

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.soundSystemLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.soundSystem, () -> {
            this.soundSystem.addActionListener(e -> this.updateVisibility());
        });

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.sampleRateLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.sampleRate);

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.bitDepthLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.bitDepth, () -> {
            this.bitDepth.setSelectedIndex(1);
        });

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.channelsLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.channels, () -> {
            this.channels.setSelectedIndex(1);
        });

        GBC.create(root).grid(0, gridy++).insets(5, 5, 0, 5).width(1).width(2).weight(1, 1).fill(GBC.BOTH).add(() -> {
            JScrollPane scrollPane = new JScrollPane();
            this.progressPanel = new ScrollPaneSizedPanel(scrollPane);
            this.progressPanel.setLayout(new VerticalLayout(5, 5));
            scrollPane.setViewportView(this.progressPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            return scrollPane;
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        GBC.create(root).grid(0, gridy++).insets(0, 0, 0, 0).weightx(1).width(2).fill(GBC.BOTH).add(bottomPanel);

        GBC.create(bottomPanel).grid(0, 0).insets(5, 5, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.progressBar, () -> {
            this.progressBar.setStringPainted(true);
        });
        GBC.create(bottomPanel).grid(1, 0).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(this.exportButton, () -> {
            this.exportButton.addActionListener(e -> this.export());
        });
    }

    private void updateVisibility() {
        boolean isNbs = this.format.getSelectedIndex() == 0;
        boolean isJavax = this.soundSystem.getSelectedIndex() == 1;

        this.soundSystemLabel.setVisible(!isNbs);
        this.soundSystem.setVisible(!isNbs);

        this.sampleRateLabel.setVisible(!isNbs);
        this.sampleRate.setVisible(!isNbs);

        this.bitDepthLabel.setVisible(!isNbs);
        this.bitDepth.setVisible(!isNbs);

        this.channelsLabel.setVisible(!isNbs && !isJavax);
        this.channels.setVisible(!isNbs && !isJavax);
    }

    private void initFrameHandler() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (ExportFrame.this.exportThread != null && ExportFrame.this.exportThread.isAlive()) {
                    try {
                        ExportFrame.this.exportThread.interrupt();
                        ExportFrame.this.exportThread.join();
                        ExportFrame.this.exportThread = null;
                    } catch (InterruptedException ignored) {
                    }
                }
                ExportFrame.this.parent.setEnabled(true);
                ExportFrame.this.dispose();
            }
        });
    }

    private void export() {
        if (this.exportThread != null && this.exportThread.isAlive()) {
            try {
                this.exportThread.interrupt();
                this.exportThread.join();
                this.exportThread = null;
            } catch (InterruptedException ignored) {
            }

            this.format.setEnabled(true);
            this.soundSystem.setEnabled(true);
            this.sampleRate.setEnabled(true);
            this.bitDepth.setEnabled(true);
            this.channels.setEnabled(true);
            this.progressPanel.removeAll();
            this.exportButton.setText("Export");
            this.progressBar.setValue(0);
            return;
        }

        File out = this.openFileChooser();
        if (out == null) return;

        this.format.setEnabled(false);
        this.soundSystem.setEnabled(false);
        this.sampleRate.setEnabled(false);
        this.bitDepth.setEnabled(false);
        this.channels.setEnabled(false);
        this.exportButton.setText("Cancel");
        this.progressBar.setValue(0);
        this.progressBar.setMaximum(this.loadedSongs.size());

        this.exportThread = new Thread(() -> this.doExport(out), "Song Export Thread");
        this.exportThread.setDaemon(true);
        this.exportThread.start();
    }

    private File openFileChooser() {
        String extension = this.format.getSelectedItem().toString().toLowerCase();
        JFileChooser fileChooser = new JFileChooser();
        if (this.loadedSongs.size() == 1) {
            fileChooser.setDialogTitle("Export Song");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new SingleFileFilter(extension));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(extension)) file = new File(file.getParentFile(), file.getName() + "." + extension);
                return file;
            }
        } else {
            fileChooser.setDialogTitle("Export Songs");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) return fileChooser.getSelectedFile();
        }
        return null;
    }

    private void doExport(final File outFile) {
        try {
            Map<ListFrame.LoadedSong, JPanel> songPanels = new ConcurrentHashMap<>();
            SwingUtilities.invokeAndWait(() -> {
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    JPanel songPanel = new JPanel();
                    songPanel.setLayout(new GridBagLayout());
                    songPanels.put(song, songPanel);

                    this.progressPanel.add(songPanel);

                    GBC.create(songPanel).grid(0, 0).insets(0).anchor(GBC.LINE_START).add(new JLabel(song.getSong().getView().getTitle()));
                    GBC.create(songPanel).grid(1, 0).insets(0, 5, 0, 0).weightx(1).fill(GBC.HORIZONTAL).add(new JProgressBar(), p -> {
                        p.setStringPainted(true);
                    });
                }
                this.progressPanel.revalidate();
                this.progressPanel.repaint();
            });

            AudioFormat format = new AudioFormat(
                    ((Number) this.sampleRate.getValue()).floatValue(),
                    Integer.parseInt(this.bitDepth.getSelectedItem().toString()),
                    this.soundSystem.getSelectedIndex() == 1 ? 1 : this.channels.getSelectedIndex() + 1,
                    true,
                    false
            );
            if (this.soundSystem.getSelectedIndex() == 0 && this.format.getSelectedIndex() != 0) OpenALSoundSystem.initCapture(8192, format);
            if (this.loadedSongs.size() == 1) {
                JPanel songPanel = songPanels.get(this.loadedSongs.get(0));
                JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                this.exportSong(this.loadedSongs.get(0), format, outFile, progress -> {
                    SwingUtilities.invokeLater(() -> {
                        int value = (int) (progress * 100);
                        progressBar.setValue(value);
                        progressBar.revalidate();
                        progressBar.repaint();
                    });
                });
                songPanels.remove(this.loadedSongs.get(0));
                SwingUtilities.invokeLater(() -> {
                    this.progressPanel.remove(songPanel);
                    this.progressPanel.revalidate();
                    this.progressPanel.repaint();
                });
            } else {
                int threadCount;
                if (this.format.getSelectedIndex() == 0) threadCount = 1;
                else if (this.soundSystem.getSelectedIndex() == 0) threadCount = 1;
                else threadCount = Math.min(this.loadedSongs.size(), Runtime.getRuntime().availableProcessors());
                ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);

                String extension = this.format.getSelectedItem().toString().toLowerCase();
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    threadPool.submit(() -> {
                        File file = new File(outFile, song.getFile().getName().substring(0, song.getFile().getName().lastIndexOf('.')) + "." + extension);
                        JPanel songPanel = songPanels.get(song);
                        JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                        this.exportSong(song, format, file, progress -> {
                            SwingUtilities.invokeLater(() -> {
                                int value = (int) (progress * 100);
                                progressBar.setValue(value);
                                progressBar.revalidate();
                                progressBar.repaint();
                            });
                        });
                        songPanels.remove(song);
                        SwingUtilities.invokeLater(() -> {
                            progressPanel.remove(songPanel);
                            this.progressPanel.revalidate();
                            this.progressPanel.repaint();

                            this.progressBar.setValue(this.loadedSongs.size() - songPanels.size());
                            this.progressBar.revalidate();
                            this.progressBar.repaint();
                        });
                    });
                }

                while (threadPool.getCompletedTaskCount() < threadPool.getTaskCount()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        threadPool.shutdownNow();
                        return;
                    }
                }
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export songs:\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (this.soundSystem.getSelectedIndex() == 0 && this.format.getSelectedIndex() != 0) OpenALSoundSystem.destroy();
            SwingUtilities.invokeLater(() -> {
                this.format.setEnabled(true);
                this.soundSystem.setEnabled(true);
                this.sampleRate.setEnabled(true);
                this.bitDepth.setEnabled(true);
                this.channels.setEnabled(true);
                this.progressPanel.removeAll();
                this.progressPanel.revalidate();
                this.progressPanel.repaint();
                this.exportButton.setText("Export");
                this.progressBar.setValue(this.loadedSongs.size());
                this.progressBar.revalidate();
                this.progressBar.repaint();
            });
        }
    }

    private void exportSong(final ListFrame.LoadedSong song, final AudioFormat format, final File file, final Consumer<Float> progressConsumer) {
        if (this.format.getSelectedIndex() == 0) {
            this.writeNbsSong(song, file);
        } else {
            SongView<?> songView = song.getSong().getView().clone();
            if (song.getSong() instanceof NbsSong) {
                SongResampler.applyNbsTempoChangers((NbsSong) song.getSong(), (SongView<NbsNote>) songView);
            }

            AudioExporter exporter;
            if (this.soundSystem.getSelectedIndex() == 0) exporter = new OpenALAudioExporter(songView, format, progressConsumer);
            else exporter = new JavaxAudioExporter(songView, format, progressConsumer);

            try {
                exporter.render();
                exporter.write(file);
            } catch (InterruptedException ignored) {
            } catch (Throwable t) {
                t.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to export song:\n" + song.getFile().getAbsolutePath() + "\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void writeNbsSong(final ListFrame.LoadedSong song, final File file) {
        try {
            final Song<?, ?, ?> exportSong = NoteBlockLib.createSongFromView(song.getSong().getView(), SongFormat.NBS);
            final NbsSong exportNbsSong = (NbsSong) exportSong;
            final NbsHeader exportNbsHeader = exportNbsSong.getHeader();
            if (song.getSong() instanceof NbsSong) {
                final NbsSong nbsSong = (NbsSong) song.getSong();
                final NbsHeader nbsHeader = ((NbsSong) song.getSong()).getHeader();
                exportNbsHeader.setVersion((byte) Math.max(nbsHeader.getVersion(), exportNbsHeader.getVersion()));
                exportNbsHeader.setAuthor(nbsHeader.getAuthor());
                exportNbsHeader.setOriginalAuthor(nbsHeader.getOriginalAuthor());
                exportNbsHeader.setDescription(nbsHeader.getDescription());
                exportNbsHeader.setAutoSave(nbsHeader.isAutoSave());
                exportNbsHeader.setAutoSaveInterval(nbsHeader.getAutoSaveInterval());
                exportNbsHeader.setTimeSignature(nbsHeader.getTimeSignature());
                exportNbsHeader.setMinutesSpent(nbsHeader.getMinutesSpent());
                exportNbsHeader.setLeftClicks(nbsHeader.getLeftClicks());
                exportNbsHeader.setRightClicks(nbsHeader.getRightClicks());
                exportNbsHeader.setNoteBlocksAdded(nbsHeader.getNoteBlocksAdded());
                exportNbsHeader.setNoteBlocksRemoved(nbsHeader.getNoteBlocksRemoved());
                exportNbsHeader.setSourceFileName(nbsHeader.getSourceFileName());
                exportNbsHeader.setLoop(nbsHeader.isLoop());
                exportNbsHeader.setMaxLoopCount(nbsHeader.getMaxLoopCount());
                exportNbsHeader.setLoopStartTick(nbsHeader.getLoopStartTick());
                exportNbsSong.getData().setCustomInstruments(nbsSong.getData().getCustomInstruments());
            } else if (song.getSong() instanceof McSpSong) {
                final McSpHeader mcSpHeader = ((McSpSong) song.getSong()).getHeader();
                exportNbsHeader.setAuthor(mcSpHeader.getAuthor());
                exportNbsHeader.setOriginalAuthor(mcSpHeader.getOriginalAuthor());
                exportNbsHeader.setAutoSave(mcSpHeader.getAutoSaveInterval() != 0);
                exportNbsHeader.setAutoSaveInterval((byte) mcSpHeader.getAutoSaveInterval());
                exportNbsHeader.setMinutesSpent(mcSpHeader.getMinutesSpent());
                exportNbsHeader.setLeftClicks(mcSpHeader.getLeftClicks());
                exportNbsHeader.setRightClicks(mcSpHeader.getRightClicks());
                exportNbsHeader.setNoteBlocksAdded(mcSpHeader.getNoteBlocksAdded());
                exportNbsHeader.setNoteBlocksRemoved(mcSpHeader.getNoteBlocksRemoved());
            }
            NoteBlockLib.writeSong(exportSong, file);
        } catch (Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export song:\n" + song.getFile().getAbsolutePath() + "\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}
