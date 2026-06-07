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

import net.lenni0451.commons.swing.GBC;
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.raphimc.audiomixer.util.FloatAudioFormat;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.renderer.SongRenderer;
import net.raphimc.noteblocktool.audio.renderer.impl.RealtimeSongRenderer;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.NewLineLabel;
import net.raphimc.noteblocktool.video.visualizer.VisualizerWindow;
import net.raphimc.noteblocktool.video.visualizer.impl.DropVisualizer;
import net.raphimc.noteblocktool.video.window.impl.AwtImpl;
import net.raphimc.noteblocktool.video.window.impl.GlfwImpl;
import org.lwjgl.system.Platform;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;

public class SongPlayerFrame extends JFrame {

    private static final String VISUALIZER_UNAVAILABLE_MESSAGE = "An error occurred while initializing the visualizer window.\nPlease make sure that your system supports at least OpenGL 4.1.";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final FloatAudioFormat PLAYBACK_AUDIO_FORMAT = new FloatAudioFormat(48000, 2);
    private static SongPlayerFrame instance;
    private static Point lastPosition;
    private static int lastVolume = 50;
    private static boolean lastTimingJitter = false;
    private static int lastMaxSounds = 4096;
    private static boolean lastThreaded = false;

    public static void open(final Song song) {
        if (instance != null && instance.isVisible()) {
            lastPosition = instance.getLocation();
            lastVolume = instance.volume.getValue();
            lastTimingJitter = instance.timingJitter.isSelected();
            lastMaxSounds = (int) instance.maxSounds.getValue();
            lastThreaded = instance.threaded.isSelected();
            instance.dispose();
        }
        SwingUtilities.invokeLater(() -> {
            instance = new SongPlayerFrame(song);
            if (lastPosition != null) instance.setLocation(lastPosition);
            instance.volume.setValue(lastVolume);
            instance.timingJitter.setSelected(lastTimingJitter);
            instance.maxSounds.setValue(lastMaxSounds);
            instance.threaded.setSelected(lastThreaded);
            instance.playStop.doClick(0);
            instance.setVisible(true);
        });
    }


    private final Song song;
    private final Timer updateTimer;
    private final JSlider volume = new JSlider(0, 100, lastVolume);
    private final JCheckBox timingJitter = new JCheckBox("Artificial Timing Jitter", lastTimingJitter);
    private final JSpinner maxSounds = new JSpinner(new SpinnerNumberModel(lastMaxSounds, 64, 131070, 64));
    private final JCheckBox threaded = new JCheckBox("Multithreaded Rendering", lastThreaded);
    private final JButton playStop = new JButton("Play");
    private final JButton pauseResume = new JButton("Pause");
    private final JButton openVisualizer = new JButton("Open Visualizer");
    private final JSlider progress = new JSlider(0, 100, 0);
    private final JLabel statusLine = new JLabel(" ");
    private final JLabel progressLabel = new JLabel("Current Position: 00:00:00");
    private SongRenderer songRenderer;
    private int currentMaxSounds;
    private boolean currentThreaded;
    private VisualizerWindow visualizerWindow;

    private SongPlayerFrame(final Song song) {
        this.song = song;
        this.updateTimer = new Timer(50, e -> this.tick());
        this.updateTimer.start();

        this.setTitle("NoteBlockTool Song Player - " + song.getTitleOrFileNameOr("No Title"));
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setSize(500, 450);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);

        this.initComponents();
        this.initFrameHandler();

        this.setMinimumSize(this.getSize());
    }

    private void initComponents() {
        final JPanel root = new JPanel();
        root.setLayout(new BorderLayout());
        this.setContentPane(root);

        { //Center Panel
            final JScrollPane centerScrollPane = new FastScrollPane();
            final JPanel centerPanel = new ScrollPaneSizedPanel(centerScrollPane);
            centerScrollPane.setViewportView(centerPanel);
            centerPanel.setLayout(new GridBagLayout());
            root.add(centerScrollPane, BorderLayout.CENTER);

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Title:"));
            GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getTitleOrFileNameOr("No Title")));

            if (this.song.getAuthor() != null) {
                GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Author:"));
                GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getAuthor()));
            }

            if (this.song.getOriginalAuthor() != null) {
                GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Original Author:"));
                GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getOriginalAuthor()));
            }

            if (this.song.getDescription() != null) {
                GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Description:"));
                GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getDescription()));
            }

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Length:"));
            GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getHumanReadableLength()));

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Note count:"));
            GBC.create(centerPanel).nextColumn().insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(DECIMAL_FORMAT.format(this.song.getNotes().getNoteCount())));

            GBC.create(centerPanel).nextRow().insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Tempo:"));
            GBC.create(centerPanel).nextColumn().insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getTempoEvents().getHumanReadableTempoRange() + " TPS"));

            GBC.fillVerticalSpace(centerPanel);
        }
        { //South Panel
            final JPanel southPanel = new JPanel();
            southPanel.setLayout(new GridBagLayout());
            root.add(southPanel, BorderLayout.SOUTH);

            GBC.create(southPanel).nextRow().anchor(GBC.CENTER).weightx(1).width(2).fill(GBC.HORIZONTAL).add(new JSeparator());

            GBC.create(southPanel).nextRow().anchor(GBC.CENTER).width(2).add(this.progressLabel);

            GBC.create(southPanel).nextRow().insets(0, 5, 0, 5).weightx(1).width(2).fill(GBC.HORIZONTAL).add(this.progress, () -> {
                this.progress.addChangeListener(e -> {
                    if (!this.progress.getValueIsAdjusting()) { // Skip updates if the value is set directly
                        return;
                    }
                    if (this.songRenderer != null) {
                        if (!this.songRenderer.isRunning()) {
                            this.songRenderer.start();
                            this.songRenderer.setPaused(true);
                        }
                        this.songRenderer.setTick(this.progress.getValue());
                    }
                });
            });

            GBC.create(southPanel).nextRow().insets(5, 5, 0, 5).weightx(1).width(2).fill(GBC.HORIZONTAL).add(new JPanel(), buttonPanel -> {
                buttonPanel.setLayout(new GridLayout(1, 3, 5, 0));
                buttonPanel.add(this.playStop);
                this.playStop.addActionListener(e -> {
                    this.initSongPlayer();
                    if (this.songRenderer.isRunning()) {
                        this.songRenderer.stop();
                        this.songRenderer.stopAllSounds();
                        this.songRenderer.setTick(0);
                    } else {
                        this.songRenderer.start();
                    }
                });
                buttonPanel.add(this.pauseResume);
                this.pauseResume.addActionListener(e -> {
                    if (this.songRenderer != null) {
                        this.songRenderer.setPaused(!this.songRenderer.isPaused());
                    }
                });
                buttonPanel.add(this.openVisualizer);
                this.openVisualizer.addActionListener(e -> {
                    if (this.visualizerWindow == null) {
                        this.visualizerWindow = new VisualizerWindow(Platform.get() != Platform.MACOSX ? GlfwImpl::new : AwtImpl::new);
                    }
                    if (!this.visualizerWindow.isOpen()) {
                        this.visualizerWindow.getConfiguration().setWindowTitle("NoteBlockTool Song Visualizer - " + this.songRenderer.getSong().getTitleOrFileNameOr("No Title"));
                        final SongRenderer finalSongRenderer = this.songRenderer;
                        this.visualizerWindow.setVisualizer(() -> new DropVisualizer(finalSongRenderer));
                        try {
                            this.visualizerWindow.open();
                            this.toFront();
                        } catch (Throwable t) {
                            this.visualizerWindow = null;
                            JOptionPane.showMessageDialog(this, VISUALIZER_UNAVAILABLE_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        this.visualizerWindow.close();
                    }
                });
            });

            GBC.create(southPanel).nextRow().insets(5, 5, 5, 5).weightx(1).width(2).fill(GBC.HORIZONTAL).add(new JPanel(), controls -> {
                controls.setLayout(new GridLayout(1, 2, 5, 5));
                {
                    JPanel playbackPanel = new JPanel(new GridBagLayout());
                    playbackPanel.setBorder(BorderFactory.createTitledBorder("Playback"));
                    controls.add(playbackPanel);

                    GBC.create(playbackPanel).nextRow().insets(0, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Volume:"));
                    GBC.create(playbackPanel).nextRow().insets(0, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.volume, () -> {
                        this.volume.setMajorTickSpacing(20);
                        this.volume.setMinorTickSpacing(5);
                        this.volume.setPaintLabels(true);
                        this.volume.setPaintTicks(true);
                        this.volume.addChangeListener(e -> {
                            if (this.songRenderer != null) {
                                this.songRenderer.setMasterVolume(this.volume.getValue());
                            }
                            lastVolume = this.volume.getValue();
                        });
                    });

                    GBC.create(playbackPanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.timingJitter, () -> {
                        this.timingJitter.setToolTipText("Adds slight timing jitter (±1ms) to make the song sound more natural and less artificial.\nThis emulates the behaviour of playing the song in Note Block Studio.");
                        this.timingJitter.addChangeListener(e -> {
                            if (this.songRenderer != null) {
                                this.songRenderer.setTimingJitter(this.timingJitter.isSelected());
                            }
                            lastTimingJitter = this.timingJitter.isSelected();
                        });
                    });

                    GBC.fillVerticalSpace(playbackPanel);
                }
                {
                    JPanel rendererPanel = new JPanel(new GridBagLayout());
                    rendererPanel.setBorder(BorderFactory.createTitledBorder("Renderer"));
                    controls.add(rendererPanel);

                    GBC.create(rendererPanel).nextRow().insets(0, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Max Sounds:"));
                    GBC.create(rendererPanel).nextRow().insets(0, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.maxSounds, () -> {
                        this.maxSounds.addChangeListener(e -> lastMaxSounds = (int) this.maxSounds.getValue());
                    });

                    GBC.create(rendererPanel).nextRow().insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(this.threaded, () -> {
                        this.threaded.addChangeListener(e -> lastThreaded = this.threaded.isSelected());
                    });

                    GBC.fillVerticalSpace(rendererPanel);
                }
            });

            GBC.create(southPanel).nextRow().weightx(1).width(2).fill(GBC.HORIZONTAL).add(new JPanel(), statusBar -> {
                statusBar.setBorder(BorderFactory.createEtchedBorder());
                statusBar.setLayout(new GridLayout(1, 1));
                statusBar.add(this.statusLine);
            });
        }
    }

    private void initSongPlayer() {
        final int maxSounds = (int) this.maxSounds.getValue();
        final boolean threaded = this.threaded.isSelected();
        if (this.songRenderer == null || this.currentMaxSounds != maxSounds || this.currentThreaded != threaded) {
            this.closeSongPlayerAndVisualizer();
            this.songRenderer = new RealtimeSongRenderer(this.song, maxSounds, true, threaded, PLAYBACK_AUDIO_FORMAT);
            this.songRenderer.setMasterVolume(this.volume.getValue());
            this.songRenderer.setTimingJitter(this.timingJitter.isSelected());
            this.currentMaxSounds = maxSounds;
            this.currentThreaded = threaded;
        }
    }

    private void initFrameHandler() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SongPlayerFrame.this.dispose();
                lastPosition = SongPlayerFrame.this.getLocation();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                SongPlayerFrame.this.updateTimer.stop();
                SongPlayerFrame.this.closeSongPlayerAndVisualizer();
            }
        });
    }

    private void tick() {
        if (this.songRenderer != null && this.songRenderer.isRunning()) {
            this.maxSounds.setEnabled(false);
            this.threaded.setEnabled(false);
            this.pauseResume.setEnabled(true);
            this.progress.setEnabled(true);
            this.playStop.setText("Stop");
            this.pauseResume.setText(this.songRenderer.isPaused() ? "Resume" : "Pause");

            final int tickCount = this.songRenderer.getSong().getNotes().getLengthInTicks();
            if (this.progress.getMaximum() != tickCount) {
                this.progress.setMaximum(tickCount);
            }
            this.progress.setValue(this.songRenderer.getTick());

            final int seconds = (int) Math.ceil(this.songRenderer.getMillisecondPosition() / 1000F);
            this.progressLabel.setText("Current Position: " + String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60));
            this.statusLine.setText(String.join(", ", this.songRenderer.getStatusLines()));
        } else {
            this.maxSounds.setEnabled(true);
            this.threaded.setEnabled(true);
            this.pauseResume.setEnabled(false);
            this.progress.setEnabled(false);
            this.playStop.setText("Play");
            this.pauseResume.setText("Pause");
            this.progress.setValue(0);
            this.progressLabel.setText(" ");
            this.statusLine.setText(" ");
        }
        this.openVisualizer.setEnabled(this.songRenderer != null);
        this.openVisualizer.setText(this.visualizerWindow != null && this.visualizerWindow.isOpen() ? "Close Visualizer" : "Open Visualizer");
    }

    private void closeSongPlayerAndVisualizer() {
        if (this.visualizerWindow != null) {
            this.visualizerWindow.close();
            this.visualizerWindow = null;
        }
        if (this.songRenderer != null) {
            this.songRenderer.close();
            this.songRenderer = null;
        }
    }


}
