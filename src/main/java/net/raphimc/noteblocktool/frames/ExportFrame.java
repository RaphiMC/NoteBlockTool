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
import net.raphimc.noteblocktool.audio.player.impl.ProgressSongRenderer;
import net.raphimc.noteblocktool.audio.player.impl.SongRenderer;
import net.raphimc.noteblocktool.audio.system.impl.AudioMixerAudioSystem;
import net.raphimc.noteblocktool.audio.system.impl.BassAudioSystem;
import net.raphimc.noteblocktool.audio.system.impl.OpenALAudioSystem;
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

    private static final int MAX_SOUNDS = 16384;

    private final ListFrame parent;
    private final List<ListFrame.LoadedSong> loadedSongs;
    private final JComboBox<OutputFormat> format = new JComboBox<>(OutputFormat.values());
    private final JLabel audioSystemLabel = new JLabel("Audio System:");
    private final JComboBox<AudioRendererType> audioSystem = new JComboBox<>(AudioRendererType.values());
    private final JCheckBox audioMixerGlobalNormalization = new JCheckBox("Global Normalization");
    private final JCheckBox audioMixerThreaded = new JCheckBox("Multithreaded Rendering");
    private final JLabel sampleRateLabel = new JLabel("Sample Rate:");
    private final JSpinner sampleRate = new JSpinner(new SpinnerNumberModel(48000, 8000, 192000, 8000));
    private final JLabel bitDepthLabel = new JLabel("PCM Bit Depth:");
    private final JComboBox<BitDepth> bitDepth = new JComboBox<>(BitDepth.values());
    private final JLabel channelsLabel = new JLabel("Channels:");
    private final JComboBox<Channels> channels = new JComboBox<>(Channels.values());
    private final JLabel volumeLabel = new JLabel("Volume:");
    private final JSlider volume = new JSlider(0, 100, 50);
    private final JCheckBox timingJitter = new JCheckBox("Artificial Timing Jitter");
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

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.audioSystemLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.audioSystem, () -> {
            this.audioSystem.addActionListener(e -> this.updateVisibility());
        });

        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).anchor(GBC.LINE_START).add(this.audioMixerGlobalNormalization);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).anchor(GBC.LINE_START).add(this.audioMixerThreaded);

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

        GBC.create(root).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(this.volumeLabel);
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.volume, () -> {
            this.volume.setMajorTickSpacing(10);
            this.volume.setMinorTickSpacing(5);
            this.volume.setPaintTicks(true);
            this.volume.setPaintLabels(true);
        });
        GBC.create(root).grid(1, gridy++).insets(5, 0, 0, 5).anchor(GBC.LINE_START).add(this.timingJitter, () -> {
            this.timingJitter.setToolTipText("Adds slight timing jitter (±1ms) to make the song sound more natural and less artificial.\nThis emulates the behaviour of playing the song in Note Block Studio.");
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
        GBC.create(root).grid(0, gridy++).insets(0, 0, 0, 0).weightx(1).width(2).fill(GBC.HORIZONTAL).add(bottomPanel);

        GBC.create(bottomPanel).grid(0, 0).insets(5, 5, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.progressBar, () -> {
            this.progressBar.setStringPainted(true);
        });
        GBC.create(bottomPanel).grid(1, 0).insets(5, 0, 5, 5).anchor(GBC.LINE_END).add(this.exportButton, () -> {
            this.exportButton.addActionListener(e -> this.export());
        });
    }

    private void updateVisibility() {
        final OutputFormat outputFormat = (OutputFormat) this.format.getSelectedItem();
        final AudioRendererType audioRendererType = (AudioRendererType) this.audioSystem.getSelectedItem();

        this.audioSystemLabel.setVisible(outputFormat.isAudioFile());
        this.audioSystem.setVisible(outputFormat.isAudioFile());

        this.audioMixerGlobalNormalization.setVisible(outputFormat.isAudioFile() && audioRendererType.equals(AudioRendererType.AUDIO_MIXER));
        this.audioMixerThreaded.setVisible(outputFormat.isAudioFile() && audioRendererType.equals(AudioRendererType.AUDIO_MIXER));

        this.sampleRateLabel.setVisible(outputFormat.isAudioFile());
        this.sampleRate.setVisible(outputFormat.isAudioFile());

        this.bitDepthLabel.setVisible(outputFormat.isAudioFile() && !outputFormat.equals(OutputFormat.MP3));
        this.bitDepth.setVisible(outputFormat.isAudioFile() && !outputFormat.equals(OutputFormat.MP3));

        this.channelsLabel.setVisible(outputFormat.isAudioFile());
        this.channels.setVisible(outputFormat.isAudioFile());

        this.volumeLabel.setVisible(outputFormat.isAudioFile());
        this.volume.setVisible(outputFormat.isAudioFile());
        this.timingJitter.setVisible(outputFormat.isAudioFile());
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
            this.audioSystem.setEnabled(true);
            this.audioMixerGlobalNormalization.setEnabled(true);
            this.audioMixerThreaded.setEnabled(true);
            this.sampleRate.setEnabled(true);
            this.bitDepth.setEnabled(true);
            this.channels.setEnabled(true);
            this.volume.setEnabled(true);
            this.timingJitter.setEnabled(true);
            this.progressPanel.removeAll();
            this.exportButton.setText("Export");
            this.progressBar.setValue(0);
            return;
        }

        File out = this.openFileChooser();
        if (out == null) return;

        this.format.setEnabled(false);
        this.audioSystem.setEnabled(false);
        this.audioMixerGlobalNormalization.setEnabled(false);
        this.audioMixerThreaded.setEnabled(false);
        this.sampleRate.setEnabled(false);
        this.bitDepth.setEnabled(false);
        this.channels.setEnabled(false);
        this.volume.setEnabled(false);
        this.timingJitter.setEnabled(false);
        this.progressPanel.removeAll();
        this.exportButton.setText("Cancel");
        this.progressBar.setValue(0);
        this.progressBar.setMaximum(this.loadedSongs.size());

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
        final boolean audioMixerThreaded = this.audioSystem.getSelectedItem().equals(AudioRendererType.AUDIO_MIXER) && this.audioMixerThreaded.isSelected();
        final boolean forceSingleThreaded = !((OutputFormat) this.format.getSelectedItem()).isAudioFile() || this.audioSystem.getSelectedItem().equals(AudioRendererType.BASS) || audioMixerThreaded;
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
                this.format.setEnabled(true);
                this.audioSystem.setEnabled(true);
                this.audioMixerGlobalNormalization.setEnabled(true);
                this.audioMixerThreaded.setEnabled(true);
                this.sampleRate.setEnabled(true);
                this.bitDepth.setEnabled(true);
                this.channels.setEnabled(true);
                this.volume.setEnabled(true);
                this.timingJitter.setEnabled(true);
                this.exportButton.setText("Export");
                this.progressBar.setValue(this.loadedSongs.size());
                this.progressBar.revalidate();
                this.progressBar.repaint();
            });
        }
    }

    private void exportSong(final ListFrame.LoadedSong song, final File file, final FloatConsumer progressConsumer) throws InterruptedException, IOException {
        OutputFormat outputFormat = (OutputFormat) this.format.getSelectedItem();
        if (outputFormat.isSongFile()) {
            this.writeSong(song, file, outputFormat.getSongFormat());
        } else if (outputFormat.isAudioFile()) {
            final AudioFormat audioFormat = new AudioFormat(
                    ((Number) this.sampleRate.getValue()).floatValue(),
                    ((BitDepth) this.bitDepth.getSelectedItem()).getBitDepth(),
                    ((Channels) this.channels.getSelectedItem()).getChannels(),
                    true,
                    false
            );
            final PcmFloatAudioFormat renderAudioFormat = new PcmFloatAudioFormat(audioFormat);

            final SongRenderer songRenderer = switch ((AudioRendererType) this.audioSystem.getSelectedItem()) {
                case OPENAL -> new ProgressSongRenderer(song.song(), progressConsumer, soundData -> new OpenALAudioSystem(soundData, MAX_SOUNDS, renderAudioFormat));
                case AUDIO_MIXER -> new ProgressSongRenderer(song.song(), progressConsumer, soundData -> new AudioMixerAudioSystem(soundData, MAX_SOUNDS, !this.audioMixerGlobalNormalization.isSelected(), this.audioMixerThreaded.isSelected(), renderAudioFormat));
                case BASS -> new ProgressSongRenderer(song.song(), progressConsumer, soundData -> new BassAudioSystem(soundData, MAX_SOUNDS, renderAudioFormat));
            };
            songRenderer.getAudioSystem().setMasterVolume(this.volume.getValue() / 100F);
            songRenderer.setTimingJitter(this.timingJitter.isSelected());
            final float[] samples;
            try {
                samples = songRenderer.renderSong();
            } finally {
                songRenderer.close();
            }
            if (this.audioSystem.getSelectedItem() == AudioRendererType.AUDIO_MIXER && this.audioMixerGlobalNormalization.isSelected()) {
                SoundSampleUtil.normalize(samples);
            }
            if (outputFormat.equals(OutputFormat.WAV) || outputFormat.equals(OutputFormat.AIF)) {
                progressConsumer.accept(101F);
                final AudioInputStream audioInputStream = AudioIO.createAudioInputStream(samples, audioFormat);
                AudioSystem.write(audioInputStream, outputFormat.equals(OutputFormat.WAV) ? AudioFileFormat.Type.WAVE : AudioFileFormat.Type.AIFF, file);
                audioInputStream.close();
            } else if (outputFormat.equals(OutputFormat.MP3)) {
                progressConsumer.accept(200F);
                final FileOutputStream fos = new FileOutputStream(file);
                final int numSamples = samples.length / audioFormat.getChannels();
                final byte[] mp3Buffer = new byte[(int) (1.25 * numSamples + 7200)];
                final Pointer lame = LameLibrary.INSTANCE.lame_init();
                if (lame == null) {
                    throw new IllegalStateException("Failed to initialize LAME encoder");
                }
                int result = LameLibrary.INSTANCE.lame_set_in_samplerate(lame, (int) audioFormat.getSampleRate());
                if (result < 0) {
                    throw new IllegalStateException("Failed to set sample rate: " + result);
                }
                result = LameLibrary.INSTANCE.lame_set_num_channels(lame, audioFormat.getChannels());
                if (result < 0) {
                    throw new IllegalStateException("Failed to set channels: " + result);
                }
                result = LameLibrary.INSTANCE.lame_init_params(lame);
                if (result < 0) {
                    throw new IllegalStateException("Failed to initialize LAME parameters: " + result);
                }
                result = LameLibrary.INSTANCE.lame_encode_buffer_interleaved_ieee_float(lame, samples, numSamples, mp3Buffer, mp3Buffer.length);
                if (result < 0) {
                    throw new IllegalStateException("Failed to encode buffer: " + result);
                }
                progressConsumer.accept(101F);
                fos.write(mp3Buffer, 0, result);
                result = LameLibrary.INSTANCE.lame_encode_flush(lame, mp3Buffer, mp3Buffer.length);
                if (result < 0) {
                    throw new IllegalStateException("Failed to flush encoder: " + result);
                }
                fos.write(mp3Buffer, 0, result);
                LameLibrary.INSTANCE.lame_close(lame);
                fos.close();
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
        WAV("WAV", "wav", null),
        AIF("AIF", "aif", null);

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

    private enum AudioRendererType {
        OPENAL("OpenAL (best sound quality, fastest)"),
        AUDIO_MIXER("AudioMixer"),
        BASS("Un4seen BASS");

        private final String name;

        AudioRendererType(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum BitDepth {
        PCM8("PCM 8", 8),
        PCM16("PCM 16", 16),
        PCM24("PCM 24", 24),
        PCM32("PCM 32", 32);

        private final String name;
        private final int bitDepth;

        BitDepth(final String name, final int bitDepth) {
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
