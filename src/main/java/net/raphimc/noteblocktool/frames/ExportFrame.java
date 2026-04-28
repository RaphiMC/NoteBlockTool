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

import com.sun.jna.Pointer;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.lenni0451.commons.swing.layouts.VerticalLayout;
import net.raphimc.audiomixer.io.AudioIO;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.audiomixer.util.SoundSampleUtil;
import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.library.LameLibrary;
import net.raphimc.noteblocktool.audio.renderer.SongRenderer;
import net.raphimc.noteblocktool.audio.renderer.impl.ProgressSongRenderer;
import net.raphimc.noteblocktool.audio.util.LameException;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.VerticalFileChooser;
import net.raphimc.noteblocktool.util.filefilter.SingleFileFilter;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private final JLabel wavBitDepthLabel = new JLabel("WAV Bit Depth:");
    private final JComboBox<WavBitDepth> wavBitDepth = new JComboBox<>(WavBitDepth.values());
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
        JPanel root = new JPanel();
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
                GBC.create(audioFilePanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.wavBitDepthLabel);
                GBC.create(audioFilePanel).nextColumn().insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.wavBitDepth, wavBitDepth -> {
                    wavBitDepth.setSelectedItem(WavBitDepth.PCM16);
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

            this.wavBitDepthLabel.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.WAV));
            this.wavBitDepth.setVisible(outputFormat.isAudioFile() && outputFormat.equals(OutputFormat.WAV));
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

            this.progressPanel.removeAll();
            this.export.setText("Export");
            this.progressBar.setValue(0);
            this.updateVisibility(true);
            return;
        }

        File out = this.openFileChooser();
        if (out == null) return;

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
        String extension = ((OutputFormat) this.format.getSelectedItem()).getExtension();
        VerticalFileChooser fileChooser = new VerticalFileChooser();
        if (this.loadedSongs.size() == 1) {
            fileChooser.setDialogTitle("Export Song");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(new SingleFileFilter(extension));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(extension)) file = new File(file.getParentFile(), file.getName() + "." + extension);
                file.getParentFile().mkdirs();
                return file;
            }
        } else {
            fileChooser.setDialogTitle("Export Songs");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                file.mkdirs();
                return file;
            }
        }
        return null;
    }

    private void doExport(final File outFile) {
        final boolean forceSingleThreaded = !((OutputFormat) this.format.getSelectedItem()).isAudioFile() || this.threaded.isSelected();
        final boolean isMp3 = this.format.getSelectedItem().equals(OutputFormat.MP3);

        try {
            if (isMp3 && !LameLibrary.isLoaded()) {
                throw new IllegalStateException("LAME MP3 encoder is not available");
            }

            Map<ListFrame.LoadedSong, JPanel> songPanels = new ConcurrentHashMap<>();
            SwingUtilities.invokeAndWait(() -> {
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    JPanel songPanel = new JPanel();
                    songPanel.setLayout(new GridBagLayout());
                    songPanels.put(song, songPanel);

                    this.progressPanel.add(songPanel);

                    GBC.create(songPanel).grid(0, 0).insets(0).anchor(GBC.LINE_START).add(new JLabel(song.song().getTitleOrFileNameOr("No Title")));
                    GBC.create(songPanel).grid(1, 0).insets(0, 5, 0, 0).weightx(1).fill(GBC.HORIZONTAL).add(new JProgressBar(), p -> p.setStringPainted(true));
                }
                this.progressPanel.revalidate();
                this.progressPanel.repaint();
            });
            final Function<JProgressBar, FloatConsumer> progressConsumer = progressBar -> progress -> SwingUtilities.invokeLater(() -> {
                int value = (int) progress;
                if (value == 200) {
                    progressBar.setString("Encoding MP3...");
                } else if (value > 100) {
                    progressBar.setString("Writing file...");
                } else {
                    progressBar.setValue(value);
                }
                progressBar.revalidate();
                progressBar.repaint();
            });

            if (this.loadedSongs.size() == 1) {
                JPanel songPanel = songPanels.get(this.loadedSongs.get(0));
                JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                try {
                    this.exportSong(this.loadedSongs.get(0), outFile, progressConsumer.apply(progressBar));
                } catch (InterruptedException ignored) {
                } catch (Throwable t) {
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
                if (forceSingleThreaded) threadCount = 1;
                else threadCount = Math.min(this.loadedSongs.size(), Runtime.getRuntime().availableProcessors());
                ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount);
                Queue<Runnable> uiQueue = new ConcurrentLinkedQueue<>();

                String extension = ((OutputFormat) this.format.getSelectedItem()).getExtension();
                for (ListFrame.LoadedSong song : this.loadedSongs) {
                    threadPool.submit(() -> {
                        JPanel songPanel = songPanels.get(song);
                        JProgressBar progressBar = (JProgressBar) songPanel.getComponent(1);
                        try {
                            File file = new File(outFile, song.file().getName().substring(0, song.file().getName().lastIndexOf('.')) + "." + extension);
                            this.exportSong(song, file, progressConsumer.apply(progressBar));
                            uiQueue.offer(() -> {
                                this.progressPanel.remove(songPanel);
                                this.progressPanel.revalidate();
                                this.progressPanel.repaint();
                            });
                        } catch (InterruptedException ignored) {
                        } catch (Throwable t) {
                            if (t.getCause() instanceof InterruptedException) {
                                return;
                            }
                            t.printStackTrace();
                            uiQueue.offer(() -> {
                                songPanel.remove(progressBar);
                                GBC.create(songPanel).grid(1, 0).insets(0, 5, 0, 0).weightx(1).fill(GBC.HORIZONTAL).add(() -> {
                                    JLabel label = new JLabel(t.getClass().getSimpleName() + ":" + t.getMessage());
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
                        while (!uiQueue.isEmpty()) uiQueue.poll().run();

                        this.progressBar.setValue(this.loadedSongs.size() - songPanels.size());
                        this.progressBar.setString((this.loadedSongs.size() - songPanels.size()) + " / " + this.loadedSongs.size());
                        this.progressBar.revalidate();
                        this.progressBar.repaint();
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                threadPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
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
        OutputFormat outputFormat = (OutputFormat) this.format.getSelectedItem();
        if (outputFormat.isSongFile()) {
            this.writeSong(song, file, outputFormat.getSongFormat());
        } else if (outputFormat.isAudioFile()) {
            final PcmFloatAudioFormat renderAudioFormat = new PcmFloatAudioFormat(((Number) this.sampleRate.getValue()).floatValue(), ((Channels) this.channels.getSelectedItem()).getChannels());
            final SongRenderer songRenderer = new ProgressSongRenderer(song.song(), (int) this.maxSounds.getValue(), !this.globalNormalization.isSelected(), this.threaded.isSelected(), renderAudioFormat, progressConsumer);
            songRenderer.setMasterVolume(this.volume.getValue() / 100F);
            songRenderer.setTimingJitter(this.timingJitter.isSelected());
            final float[] samples;
            try {
                samples = songRenderer.renderSong();
            } finally {
                songRenderer.close();
            }
            if (this.globalNormalization.isSelected()) {
                SoundSampleUtil.normalize(samples);
            }
            if (outputFormat.equals(OutputFormat.WAV)) {
                progressConsumer.accept(101F);
                final AudioFormat audioFormat = new AudioFormat(
                        ((Number) this.sampleRate.getValue()).floatValue(),
                        ((WavBitDepth) this.wavBitDepth.getSelectedItem()).getBitDepth(),
                        ((Channels) this.channels.getSelectedItem()).getChannels(),
                        true,
                        false
                );
                final AudioInputStream audioInputStream = AudioIO.createAudioInputStream(samples, audioFormat);
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
                audioInputStream.close();
            } else if (outputFormat.equals(OutputFormat.MP3)) {
                progressConsumer.accept(200F);
                final Pointer lame = LameLibrary.INSTANCE.lame_init();
                if (lame == null) {
                    throw new IllegalStateException("Failed to create LAME instance");
                }
                LameException.check(LameLibrary.INSTANCE.lame_set_in_samplerate(lame, (int) renderAudioFormat.getSampleRate()), "Failed to set sample rate");
                LameException.check(LameLibrary.INSTANCE.lame_set_num_channels(lame, renderAudioFormat.getChannels()), "Failed to set channels");
                LameException.check(LameLibrary.INSTANCE.lame_set_VBR(lame, LameLibrary.vbr_default), "Failed to set VBR mode");
                LameException.check(LameLibrary.INSTANCE.lame_set_VBR_quality(lame, (1F - (this.mp3Quality.getValue() / 100F)) * 9F), "Failed to set VBR quality");
                LameLibrary.INSTANCE.id3tag_init(lame);
                LameLibrary.INSTANCE.lame_set_write_id3tag_automatic(lame, false);
                if (songRenderer.getSong().getTitle() != null) {
                    LameLibrary.INSTANCE.id3tag_set_title(lame, songRenderer.getSong().getTitle());
                }
                if (songRenderer.getSong().getAuthor() != null) {
                    LameLibrary.INSTANCE.id3tag_set_artist(lame, songRenderer.getSong().getAuthor());
                }
                if (songRenderer.getSong().getDescription() != null) {
                    LameLibrary.INSTANCE.id3tag_set_comment(lame, songRenderer.getSong().getDescription());
                }
                LameException.check(LameLibrary.INSTANCE.id3tag_set_fieldvalue(lame, "TXXX=Renderer=NoteBlockTool"), "Failed to set custom ID3 tag");
                LameException.check(LameLibrary.INSTANCE.lame_init_params(lame), "Failed to initialize LAME instance");

                final int frameCount = samples.length / renderAudioFormat.getChannels();
                final byte[] data = new byte[MathUtils.ceilInt(1.25F * frameCount + 7200)];
                final int dataLength = LameException.check(switch (renderAudioFormat.getChannels()) {
                    case 1 -> LameLibrary.INSTANCE.lame_encode_buffer_ieee_float(lame, samples, null, frameCount, data, data.length);
                    case 2 -> LameLibrary.INSTANCE.lame_encode_buffer_interleaved_ieee_float(lame, samples, frameCount, data, data.length);
                    default -> throw new UnsupportedOperationException("Unsupported channel count: " + renderAudioFormat.getChannels());
                }, "Failed to encode buffer");
                final byte[] trailer = new byte[7200];
                final int trailerLength = LameException.check(LameLibrary.INSTANCE.lame_encode_flush(lame, trailer, trailer.length), "Failed to flush encoder");
                final byte[] lameTagFrame = new byte[LameLibrary.INSTANCE.lame_get_lametag_frame(lame, null, 0)];
                final int lameTagFrameLength = LameException.check(LameLibrary.INSTANCE.lame_get_lametag_frame(lame, lameTagFrame, lameTagFrame.length), "Failed to get LAME tag frame");
                final byte[] id3v1Tag = new byte[LameLibrary.INSTANCE.lame_get_id3v1_tag(lame, null, 0)];
                final int id3v1TagLength = LameException.check(LameLibrary.INSTANCE.lame_get_id3v1_tag(lame, id3v1Tag, id3v1Tag.length), "Failed to get ID3v1 tag");
                final byte[] id3v2Tag = new byte[LameLibrary.INSTANCE.lame_get_id3v2_tag(lame, null, 0)];
                final int id3v2TagLength = LameException.check(LameLibrary.INSTANCE.lame_get_id3v2_tag(lame, id3v2Tag, id3v2Tag.length), "Failed to get ID3v2 tag");
                LameException.check(LameLibrary.INSTANCE.lame_close(lame), "Failed to close LAME instance");

                progressConsumer.accept(101F);
                System.arraycopy(lameTagFrame, 0, data, 0, lameTagFrameLength);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(id3v2Tag, 0, id3v2TagLength);
                    fos.write(data, 0, dataLength);
                    fos.write(trailer, 0, trailerLength);
                    fos.write(id3v1Tag, 0, id3v1TagLength);
                }
            } else {
                throw new UnsupportedOperationException("Unsupported output format: " + this.format.getSelectedIndex());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported output format: " + this.format.getSelectedIndex());
        }
    }

    private void writeSong(final ListFrame.LoadedSong song, final File file, final SongFormat format) {
        try {
            final Song exportSong = NoteBlockLib.convertSong(song.song(), format);
            NoteBlockLib.writeSong(exportSong, file);
        } catch (Throwable t) {
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

    private enum WavBitDepth {
        PCM8("PCM 8", 8),
        PCM16("PCM 16", 16),
        PCM24("PCM 24", 24),
        PCM32("PCM 32", 32);

        private final String name;
        private final int bitDepth;

        WavBitDepth(final String name, final int bitDepth) {
            this.name = name;
            this.bitDepth = bitDepth;
        }

        public int getBitDepth() {
            return this.bitDepth;
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

        public int getChannels() {
            return this.channels;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}
