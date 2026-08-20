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
package net.raphimc.noteblocktool.frames;

import de.sciss.jump3r.mp3.VbrMode;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.lenni0451.commons.swing.layouts.VerticalLayout;
import net.raphimc.audiomixer.io.AudioOutputStream;
import net.raphimc.audiomixer.io.mp3.Mp3AudioOutputStream;
import net.raphimc.audiomixer.io.wav.WavPcmAudioOutputStream;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.PcmAudioFormat;
import net.raphimc.audiomixer.util.PcmSampleEncoding;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.io.seekable.SeekableBufferedOutputStream;
import net.raphimc.audiomixer.util.io.seekable.SeekableFileOutputStream;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.renderer.SongRenderer;
import net.raphimc.noteblocktool.audio.renderer.impl.ProgressSongRenderer;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.VerticalFileChooser;
import net.raphimc.noteblocktool.util.filefilter.SingleFileFilter;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

public class ExportFrame extends JFrame {

    private final ListFrame parent;
    private final List<ListFrame.LoadedSong> loadedSongs;
    private final JLabel formatLabel = new JLabel("Format:");
    private final JComboBox<OutputFormat> format = new JComboBox<>(OutputFormat.values());

    // Audio File settings
    private final JPanel audioFilePanel = new JPanel(new GridBagLayout());
    private final JSpinner sampleRate = new JSpinner(new SpinnerNumberModel(48000, 8000, 192000, 8000));
    private final JComboBox<Channels> channels = new JComboBox<>(Channels.values());
    private final JLabel wavEncodingLabel = new JLabel("WAV Encoding:");
    private final JComboBox<WavSampleEncoding> wavEncoding = new JComboBox<>(WavSampleEncoding.values());
    private final JLabel mp3EncodingLabel = new JLabel("MP3 Encoding:");
    private final JComboBox<Mp3Encoding> mp3Encoding = new JComboBox<>(Mp3Encoding.values());
    private final JLabel mp3QualityLabel = new JLabel("MP3 Quality:");
    private final JSlider mp3Quality = new JSlider(0, 100, 60);

    // Playback settings
    private final JPanel playbackPanel = new JPanel(new GridBagLayout());
    private final JSlider volume = new JSlider(0, 100, 50);
    private final JCheckBox timingJitter = new JCheckBox("Artificial Timing Jitter");

    // Renderer settings
    private final JPanel rendererPanel = new JPanel(new GridBagLayout());
    private final JSpinner maxSounds = new JSpinner(new SpinnerNumberModel(16384, 64, 131070, 64));
    private final JCheckBox globalNormalization = new JCheckBox("Global Normalization");
    private final JCheckBox threaded = new JCheckBox("Multithreaded Rendering");

    private final JPanel progressPanel = new JPanel();
    private final JProgressBar progressBar = new JProgressBar();
    private final JButton export = new JButton("Export");
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
        this.updateVisibility(true);
        this.initFrameHandler();

        this.setMinimumSize(this.getSize());
        this.setVisible(true);
    }

    private void initComponents() {
        final JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        this.setContentPane(root);

        { // North panel
            final JPanel northPanel = new JPanel(new GridBagLayout());
            root.add(northPanel, BorderLayout.NORTH);
            GBC.create(northPanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.formatLabel);
            GBC.create(northPanel).nextColumn().insets(5, 5, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.format, format -> {
                format.addActionListener(e -> this.updateVisibility(true));
            });
        }

        { // Center panel
            final JScrollPane centerScrollPane = new FastScrollPane();
            final JPanel centerPanel = new ScrollPaneSizedPanel(centerScrollPane);
            centerScrollPane.setViewportView(centerPanel);
            centerPanel.setLayout(new GridBagLayout());
            root.add(centerScrollPane, BorderLayout.CENTER);
            GBC.create(centerPanel).nextRow().insets(0, 5, 0, 5).width(2).weightx(1).fill(GBC.HORIZONTAL).add(this.audioFilePanel, audioFilePanel -> {
                audioFilePanel.setBorder(BorderFactory.createTitledBorder("Audio File"));
                GBC.create(audioFilePanel).nextRow().insets(0, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Sample Rate:"));
                GBC.create(audioFilePanel).nextColumn().insets(0, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.sampleRate);
                GBC.create(audioFilePanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Channels:"));
                GBC.create(audioFilePanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.channels, channels -> {
                    channels.setSelectedItem(Channels.STEREO);
                });
                GBC.create(audioFilePanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.wavEncodingLabel);
                GBC.create(audioFilePanel).nextColumn().insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.wavEncoding, wavBitDepth -> {
                    wavBitDepth.setSelectedItem(WavSampleEncoding.S16_LE);
                });
                GBC.create(audioFilePanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.mp3EncodingLabel);
                GBC.create(audioFilePanel).nextColumn().insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.mp3Encoding, mp3Encoding -> {
                    mp3Encoding.setSelectedItem(Mp3Encoding.VBR);
                });
                GBC.create(audioFilePanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.mp3QualityLabel);
                GBC.create(audioFilePanel).nextColumn().insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.mp3Quality, mp3Quality -> {
                    mp3Quality.setMajorTickSpacing(10);
                    mp3Quality.setMinorTickSpacing(5);
                    mp3Quality.setPaintTicks(true);
                    mp3Quality.setPaintLabels(true);
                });
            });

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).width(2).weightx(1).fill(GBC.HORIZONTAL).add(this.playbackPanel, playbackPanel -> {
                playbackPanel.setBorder(BorderFactory.createTitledBorder("Playback"));
                GBC.create(playbackPanel).nextRow().insets(0, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Volume:"));
                GBC.create(playbackPanel).nextColumn().insets(0, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.volume, volume -> {
                    volume.setMajorTickSpacing(10);
                    volume.setMinorTickSpacing(5);
                    volume.setPaintLabels(true);
                    volume.setPaintTicks(true);
                });
                GBC.create(playbackPanel).nextRow().insets(5, 5, 5, 5).width(2).anchor(GBC.LINE_START).add(this.timingJitter, timingJitter -> {
                    timingJitter.setToolTipText("Adds slight timing jitter (±1ms) to make the song sound more natural and less artificial.\nThis emulates the behaviour of playing the song in Note Block Studio.");
                });
            });

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).width(2).weightx(1).fill(GBC.HORIZONTAL).add(this.rendererPanel, rendererPanel -> {
                rendererPanel.setBorder(BorderFactory.createTitledBorder("Renderer"));
                GBC.create(rendererPanel).nextRow().insets(0, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Max Sounds:"));
                GBC.create(rendererPanel).nextColumn().insets(0, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.maxSounds);
                GBC.create(rendererPanel).nextRow().insets(5, 5, 0, 5).width(2).anchor(GBC.LINE_START).add(this.globalNormalization);
                GBC.create(rendererPanel).nextRow().insets(5, 5, 5, 5).width(2).anchor(GBC.LINE_START).add(this.threaded);
            });

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).width(1).width(2).weight(1, 1).fill(GBC.BOTH).add(this.progressPanel, progressPanel -> {
                progressPanel.setLayout(new VerticalLayout(5, 5));
            });

            GBC.fillVerticalSpace(centerPanel);
        }

        { // South panel
            final JPanel southPanel = new JPanel(new GridBagLayout());
            root.add(southPanel, BorderLayout.SOUTH);
            GBC.create(southPanel).nextRow().insets(5, 5, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.progressBar, progressBar -> {
                progressBar.setStringPainted(true);
            });
            GBC.create(southPanel).nextColumn().insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(this.export, exportButton -> {
                exportButton.addActionListener(e -> this.export());
            });
        }
    }

    private void updateVisibility(final boolean showSettings) {
        if (showSettings) {
            final OutputFormat outputFormat = (OutputFormat) this.format.getSelectedItem();
            this.formatLabel.setVisible(true);
            this.format.setVisible(true);
            this.audioFilePanel.setVisible(outputFormat.isAudioFile());
            this.playbackPanel.setVisible(outputFormat.isAudioFile());
            this.rendererPanel.setVisible(outputFormat.isAudioFile());
            this.progressPanel.setVisible(false);

            this.wavEncodingLabel.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.WAV));
            this.wavEncoding.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.WAV));
            this.mp3EncodingLabel.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.MP3));
            this.mp3Encoding.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.MP3));
            this.mp3QualityLabel.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.MP3));
            this.mp3Quality.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.MP3));
        } else {
            this.formatLabel.setVisible(false);
            this.format.setVisible(false);
            this.audioFilePanel.setVisible(false);
            this.playbackPanel.setVisible(false);
            this.rendererPanel.setVisible(false);
            this.progressPanel.setVisible(true);
        }
    }

    private void initFrameHandler() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                if (ExportFrame.this.exportThread != null && ExportFrame.this.exportThread.isAlive()) {
                    try {
                        ExportFrame.this.exportThread.interrupt();
                        ExportFrame.this.exportThread.join();
                        ExportFrame.this.exportThread = null;
                    } catch (final InterruptedException ignored) {
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
            } catch (final InterruptedException ignored) {
            }

            this.progressPanel.removeAll();
            this.export.setText("Export");
            this.progressBar.setValue(0);
            this.updateVisibility(true);
            return;
        }

        final File out = this.openFileChooser();
        if (out == null) {
            return;
        }

        this.progressPanel.removeAll();
        this.export.setText("Cancel");
        this.progressBar.setValue(0);
        this.progressBar.setMaximum(this.loadedSongs.size());
        this.updateVisibility(false);

        this.exportThread = new Thread(() -> this.doExport(out), "Song Export Thread");
        this.exportThread.setDaemon(true);
        this.exportThread.start();
    }

    private File openFileChooser() {
        final String extension = ((OutputFormat) this.format.getSelectedItem()).getExtension();
        final VerticalFileChooser fileChooser = new VerticalFileChooser();
        if (this.loadedSongs.size() == 1) {
            fileChooser.setDialogTitle("Export Song");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new SingleFileFilter(extension));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(extension)) {
                    file = new File(file.getParentFile(), file.getName() + "." + extension);
                }
                file.getParentFile().mkdirs();
                return file;
            }
        } else {
            fileChooser.setDialogTitle("Export Songs");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                file.mkdirs();
                return file;
            }
        }
        return null;
    }

    private void doExport(final File outFile) {
        try {
            final Map<ListFrame.LoadedSong, JPanel> songPanels = new ConcurrentHashMap<>();
            SwingUtilities.invokeAndWait(() -> {
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    final JPanel songPanel = new JPanel();
                    songPanel.setLayout(new GridBagLayout());
                    songPanels.put(song, songPanel);

                    this.progressPanel.add(songPanel);

                    GBC.create(songPanel).grid(0, 0).insets(0).anchor(GBC.LINE_START).add(new JLabel(song.song().getTitleOrFileNameOr("No Title")));
                    GBC.create(songPanel).grid(1, 0).insets(0, 5, 0, 0).weightx(1).fill(GBC.HORIZONTAL).add(new JProgressBar(), p -> p.setStringPainted(true));
                }
                this.progressPanel.revalidate();
                this.progressPanel.repaint();
            });
            @SuppressWarnings("checkstyle:RequireThis") final Function<JProgressBar, FloatConsumer> progressConsumer = progressBar -> progress -> SwingUtilities.invokeLater(() -> {
                final int value = (int) progress;
                if (value > 100) {
                    progressBar.setString("Writing file...");
                } else {
                    progressBar.setValue(value);
                }
                progressBar.revalidate();
                progressBar.repaint();
            });

            if (this.loadedSongs.size() == 1) {
                final JPanel songPanel = songPanels.get(this.loadedSongs.get(0));
                final JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                try {
                    this.exportSong(this.loadedSongs.get(0), outFile, progressConsumer.apply(progressBar));
                } catch (final InterruptedException ignored) {
                } catch (final Throwable t) {
                    if (t.getCause() instanceof InterruptedException) {
                        return;
                    }
                    t.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Failed to export song:\n" + this.loadedSongs.get(0).file().getAbsolutePath() + "\n" + t.getClass().getSimpleName() + ": " + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    songPanels.remove(this.loadedSongs.get(0));
                    SwingUtilities.invokeLater(() -> {
                        this.progressPanel.remove(songPanel);
                        this.progressPanel.revalidate();
                        this.progressPanel.repaint();
                    });
                }
            } else {
                final int threadCount;
                if (this.threaded.isSelected() && ((OutputFormat) this.format.getSelectedItem()).isAudioFile()) {
                    threadCount = Math.min(this.loadedSongs.size(), Runtime.getRuntime().availableProcessors());
                } else {
                    threadCount = 1;
                }
                final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
                final Queue<Runnable> uiQueue = new ConcurrentLinkedQueue<>();

                final String extension = ((OutputFormat) this.format.getSelectedItem()).getExtension();
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    threadPool.submit(() -> {
                        final JPanel songPanel = songPanels.get(song);
                        final JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                        try {
                            final File file = new File(outFile, song.file().getName().substring(0, song.file().getName().lastIndexOf('.')) + "." + extension);
                            this.exportSong(song, file, progressConsumer.apply(progressBar));
                            uiQueue.offer(() -> {
                                this.progressPanel.remove(songPanel);
                                this.progressPanel.revalidate();
                                this.progressPanel.repaint();
                            });
                        } catch (final InterruptedException ignored) {
                        } catch (final Throwable t) {
                            if (t.getCause() instanceof InterruptedException) {
                                return;
                            }
                            t.printStackTrace();
                            uiQueue.offer(() -> {
                                songPanel.remove(progressBar);
                                GBC.create(songPanel).grid(1, 0).insets(0, 5, 0, 0).weightx(1).fill(GBC.HORIZONTAL).add(() -> {
                                    final JLabel label = new JLabel(t.getClass().getSimpleName() + ":" + t.getMessage());
                                    label.setForeground(new Color(255, 107, 104));
                                    return label;
                                });
                            });
                        } finally {
                            songPanels.remove(song);
                        }
                    });
                }

                while (threadPool.getCompletedTaskCount() < threadPool.getTaskCount() || !uiQueue.isEmpty()) {
                    SwingUtilities.invokeAndWait(() -> {
                        while (!uiQueue.isEmpty()) {
                            uiQueue.poll().run();
                        }

                        this.progressBar.setValue(this.loadedSongs.size() - songPanels.size());
                        this.progressBar.setString((this.loadedSongs.size() - songPanels.size()) + " / " + this.loadedSongs.size());
                        this.progressBar.revalidate();
                        this.progressBar.repaint();
                    });
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
                threadPool.shutdownNow();
            }
        } catch (final InterruptedException ignored) {
        } catch (final Throwable t) {
            if (t.getCause() instanceof InterruptedException) {
                return;
            }
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export songs:\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            SwingUtilities.invokeLater(() -> {
                this.export.setText("Export");
                this.progressBar.setValue(this.loadedSongs.size());
                this.progressBar.revalidate();
                this.progressBar.repaint();
                this.updateVisibility(true);
            });
        }
    }

    private void exportSong(final ListFrame.LoadedSong song, final File file, final FloatConsumer progressConsumer) throws InterruptedException, IOException {
        final OutputFormat outputFormat = (OutputFormat) this.format.getSelectedItem();
        if (outputFormat.isSongFile()) {
            this.writeSong(song, file, outputFormat.getSongFormat());
        } else if (outputFormat.isAudioFile()) {
            final AudioFormat audioFormat = new AudioFormat(((Number) this.sampleRate.getValue()).floatValue(), ((Channels) this.channels.getSelectedItem()).channels());
            final SongRenderer songRenderer = new ProgressSongRenderer(song.song(), (int) this.maxSounds.getValue(), !this.globalNormalization.isSelected(), this.threaded.isSelected(), audioFormat, progressConsumer);
            songRenderer.setMasterVolume(this.volume.getValue());
            songRenderer.setTimingJitter(this.timingJitter.isSelected());
            final AudioBuffer buffer;
            try {
                buffer = songRenderer.renderSong();
            } finally {
                songRenderer.close();
            }
            if (this.globalNormalization.isSelected()) {
                buffer.limitToUnitRange();
            }
            final AudioOutputStream audioOutputStream;
            if (outputFormat.equals(OutputFormat.WAV)) {
                audioOutputStream = new WavPcmAudioOutputStream(new BufferedOutputStream(new FileOutputStream(file), 1024 * 1024), new PcmAudioFormat(buffer.format(), ((WavSampleEncoding) this.wavEncoding.getSelectedItem()).encoding()), buffer.sampleCount());
            } else if (outputFormat.equals(OutputFormat.MP3)) {
                final Mp3AudioOutputStream.Id3Metadata id3Metadata = new Mp3AudioOutputStream.Id3Metadata()
                    .withTitle(songRenderer.getSong().getTitle())
                    .withArtist(songRenderer.getSong().getAuthor())
                    .withComment(songRenderer.getSong().getDescription());
                audioOutputStream = new Mp3AudioOutputStream(new SeekableBufferedOutputStream(new SeekableFileOutputStream(file), 1024 * 1024), buffer.format(), this.mp3Quality.getValue() / 100F, ((Mp3Encoding) this.mp3Encoding.getSelectedItem()).mode(), id3Metadata);
            } else {
                throw new UnsupportedOperationException("Unsupported output format: " + this.format.getSelectedIndex());
            }
            progressConsumer.accept(101F);
            try {
                audioOutputStream.write(buffer.samples());
            } finally {
                audioOutputStream.close();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported output format: " + this.format.getSelectedIndex());
        }
    }

    private void writeSong(final ListFrame.LoadedSong song, final File file, final SongFormat format) {
        try {
            final Song exportSong = NoteBlockLib.convertSong(song.song(), format);
            NoteBlockLib.writeSong(exportSong, file);
        } catch (final Throwable t) {
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export song:\n" + song.file().getAbsolutePath() + "\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private enum OutputFormat {
        NBS("NBS", "nbs", SongFormat.NBS),
        MCSP2("MCSP2", "mcsp2", SongFormat.MCSP2),
        TXT("TXT", "txt", SongFormat.TXT),
        MP3("MP3 (Using LAME encoder)", "mp3", null),
        WAV("WAV", "wav", null);

        private final String name;
        private final String extension;
        private final SongFormat songFormat;

        OutputFormat(final String name, final String extension, final SongFormat songFormat) {
            this.name = name;
            this.extension = extension;
            this.songFormat = songFormat;
        }

        public String getExtension() {
            return this.extension;
        }

        public SongFormat getSongFormat() {
            return this.songFormat;
        }

        public boolean isSongFile() {
            return this.songFormat != null;
        }

        public boolean isAudioFile() {
            return this.songFormat == null;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum Channels {
        MONO("Mono", 1),
        STEREO("Stereo", 2);

        private final String name;
        private final int channels;

        Channels(final String name, final int channels) {
            this.name = name;
            this.channels = channels;
        }

        public int channels() {
            return this.channels;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum WavSampleEncoding {
        U8("Unsigned 8-Bit PCM", PcmSampleEncoding.U8),
        S16_LE("Signed 16-Bit PCM", PcmSampleEncoding.S16_LE),
        S24_LE("Signed 24-Bit PCM", PcmSampleEncoding.S24_LE),
        S32_LE("Signed 32-Bit PCM", PcmSampleEncoding.S32_LE),
        F32_LE("Float 32-Bit PCM", PcmSampleEncoding.F32_LE);

        private final String name;
        private final PcmSampleEncoding encoding;

        WavSampleEncoding(final String name, final PcmSampleEncoding encoding) {
            this.name = name;
            this.encoding = encoding;
        }

        public PcmSampleEncoding encoding() {
            return this.encoding;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum Mp3Encoding {
        CBR("Constant bitrate (CBR)", VbrMode.vbr_off),
        ABR("Average bitrate (ABR)", VbrMode.vbr_abr),
        VBR("Variable bitrate (VBR)", VbrMode.vbr_default);

        private final String name;
        private final VbrMode mode;

        Mp3Encoding(final String name, final VbrMode mode) {
            this.name = name;
            this.mode = mode;
        }

        public VbrMode mode() {
            return this.mode;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}
