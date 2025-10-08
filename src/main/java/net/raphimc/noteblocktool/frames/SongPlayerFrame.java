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
import net.lenni0451.commons.swing.components.ScrollPaneSizedPanel;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocktool.audio.player.AudioSystemSongPlayer;
import net.raphimc.noteblocktool.audio.player.impl.RealtimeSongPlayer;
import net.raphimc.noteblocktool.audio.player.impl.RealtimeSongRenderer;
import net.raphimc.noteblocktool.audio.system.impl.AudioMixerAudioSystem;
import net.raphimc.noteblocktool.audio.system.impl.BassAudioSystem;
import net.raphimc.noteblocktool.audio.system.impl.OpenALAudioSystem;
import net.raphimc.noteblocktool.audio.system.impl.XAudio2AudioSystem;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.NewLineLabel;
import net.raphimc.noteblocktool.frames.visualizer.VisualizerWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;

public class SongPlayerFrame extends JFrame {

    private static final String AUDIO_SYSTEM_UNAVAILABLE_MESSAGE = "An error occurred while initializing the audio system.\nPlease make sure that your system supports the selected audio system.";
    private static final String VISUALIZER_UNAVAILABLE_MESSAGE = "An error occurred while initializing the visualizer window.\nPlease make sure that your system supports at least OpenGL 4.5.";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final PcmFloatAudioFormat PLAYBACK_AUDIO_FORMAT = new PcmFloatAudioFormat(48000, 2);
    private static SongPlayerFrame instance;
    private static Point lastPosition;
    private static int lastAudioSystem;
    private static int lastMaxSounds = 1024;
    private static int lastVolume = 50;

    public static void open(final Song song) {
        if (instance != null && instance.isVisible()) {
            lastPosition = instance.getLocation();
            lastAudioSystem = instance.audioSystemComboBox.getSelectedIndex();
            lastMaxSounds = (int) instance.maxSoundsSpinner.getValue();
            lastVolume = instance.volumeSlider.getValue();
            instance.dispose();
        }
        SwingUtilities.invokeLater(() -> {
            instance = new SongPlayerFrame(song);
            if (lastPosition != null) instance.setLocation(lastPosition);
            instance.audioSystemComboBox.setSelectedIndex(lastAudioSystem);
            instance.maxSoundsSpinner.setValue(lastMaxSounds);
            instance.volumeSlider.setValue(lastVolume);
            instance.playStopButton.doClick(0);
            instance.setVisible(true);
        });
    }

    public static void close() {
        if (instance != null) instance.dispose();
    }


    private final Song song;
    private final Timer updateTimer;
    private final JComboBox<AudioSystemType> audioSystemComboBox = new JComboBox<>(AudioSystemType.values());
    private final JSpinner maxSoundsSpinner = new JSpinner(new SpinnerNumberModel(lastMaxSounds, 64, 65535, 64));
    private final JSlider volumeSlider = new JSlider(0, 100, lastVolume);
    private final JButton playStopButton = new JButton("Play");
    private final JButton pauseResumeButton = new JButton("Pause");
    private final JButton openVisualizerButton = new JButton("Open Visualizer");
    private final JSlider progressSlider = new JSlider(0, 100, 0);
    private final JLabel statusLine = new JLabel(" ");
    private final JLabel progressLabel = new JLabel("Current Position: 00:00:00");
    private AudioSystemSongPlayer songPlayer;
    private AudioSystemType audioSystemType;
    private VisualizerWindow visualizerWindow;

    private SongPlayerFrame(final Song song) {
        this.song = song;
        this.updateTimer = new Timer(50, e -> this.tick());
        this.updateTimer.start();

        this.setTitle("NoteBlockTool Song Player - " + song.getTitleOrFileNameOr("No Title"));
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setSize(500, 400);
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

        { //North Panel
            final JPanel northPanel = new JPanel();
            northPanel.setLayout(new GridBagLayout());
            root.add(northPanel, BorderLayout.NORTH);

            int gridy = 0;
            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Audio System:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.audioSystemComboBox, () -> {
                this.audioSystemComboBox.addActionListener(e -> lastAudioSystem = this.audioSystemComboBox.getSelectedIndex());
            });

            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Max Sounds:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.maxSoundsSpinner, () -> {
                this.maxSoundsSpinner.addChangeListener(e -> lastMaxSounds = (int) this.maxSoundsSpinner.getValue());
            });

            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(new JLabel("Volume:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.volumeSlider, () -> {
                this.volumeSlider.setPaintLabels(true);
                this.volumeSlider.setPaintTicks(true);
                this.volumeSlider.setMajorTickSpacing(10);
                this.volumeSlider.setMinorTickSpacing(5);
                this.volumeSlider.addChangeListener(e -> {
                    if (this.songPlayer != null) {
                        this.songPlayer.getAudioSystem().setMasterVolume(this.volumeSlider.getValue() / 100F);
                    }
                    lastVolume = this.volumeSlider.getValue();
                });
            });
        }
        { //Center Panel
            final JScrollPane centerScrollPane = new FastScrollPane();
            final JPanel centerPanel = new ScrollPaneSizedPanel(centerScrollPane);
            centerScrollPane.setViewportView(centerPanel);
            centerPanel.setLayout(new GridBagLayout());
            root.add(centerScrollPane, BorderLayout.CENTER);

            int gridy = 0;
            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Title:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getTitleOrFileNameOr("No Title")));

            if (this.song.getAuthor() != null) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Author:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getAuthor()));
            }

            if (this.song.getOriginalAuthor() != null) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Original Author:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getOriginalAuthor()));
            }

            if (this.song.getDescription() != null) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Description:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getDescription()));
            }

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Length:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getHumanReadableLength()));

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Note count:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(DECIMAL_FORMAT.format(this.song.getNotes().getNoteCount())));

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Tempo:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getTempoEvents().getHumanReadableTempoRange() + " TPS"));

            GBC.fillVerticalSpace(centerPanel);
        }
        { //South Panel
            final JPanel southPanel = new JPanel();
            southPanel.setLayout(new GridBagLayout());
            root.add(southPanel, BorderLayout.SOUTH);

            int gridy = 0;
            GBC.create(southPanel).grid(0, gridy++).anchor(GBC.CENTER).add(this.progressLabel);

            GBC.create(southPanel).grid(0, gridy++).insets(5, 5, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.progressSlider, () -> {
                this.progressSlider.addChangeListener(e -> {
                    if (!this.progressSlider.getValueIsAdjusting()) { // Skip updates if the value is set directly
                        return;
                    }
                    if (this.songPlayer != null) {
                        if (!this.songPlayer.isRunning()) {
                            this.songPlayer.start();
                            this.songPlayer.setPaused(true);
                        }
                        this.songPlayer.setTick(this.progressSlider.getValue());
                    }
                });
            });

            final JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridLayout(1, 3, 5, 0));
            buttonPanel.add(this.playStopButton);
            this.playStopButton.addActionListener(e -> {
                this.initSongPlayer();
                if (this.songPlayer != null) {
                    if (this.songPlayer.isRunning()) {
                        this.songPlayer.stop();
                        this.songPlayer.setTick(0);
                    } else {
                        this.songPlayer.start();
                    }
                }
            });
            buttonPanel.add(this.pauseResumeButton);
            this.pauseResumeButton.addActionListener(e -> {
                if (this.songPlayer != null) {
                    this.songPlayer.setPaused(!this.songPlayer.isPaused());
                }
            });
            buttonPanel.add(this.openVisualizerButton);
            this.openVisualizerButton.addActionListener(e -> {
                if (this.visualizerWindow != null) {
                    this.openVisualizerButton.setText("Open Visualizer");
                    this.visualizerWindow.close();
                    this.visualizerWindow = null;
                } else {
                    try {
                        this.visualizerWindow = new VisualizerWindow(this.songPlayer, () -> SwingUtilities.invokeLater(this::toFront), () -> SwingUtilities.invokeLater(() -> {
                            this.openVisualizerButton.setText("Open Visualizer");
                            this.visualizerWindow = null;
                        }));
                        this.openVisualizerButton.setText("Close Visualizer");
                    } catch (Throwable t) {
                        this.visualizerWindow = null;
                        JOptionPane.showMessageDialog(this, VISUALIZER_UNAVAILABLE_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            GBC.create(southPanel).grid(0, gridy++).insets(5, 5, 5, 5).weightx(1).width(2).fill(GBC.HORIZONTAL).add(buttonPanel);

            final JPanel statusBar = new JPanel();
            statusBar.setBorder(BorderFactory.createEtchedBorder());
            statusBar.setLayout(new GridLayout(1, 1));
            statusBar.add(this.statusLine);
            GBC.create(southPanel).grid(0, gridy++).weightx(1).fill(GBC.HORIZONTAL).add(statusBar);
        }
    }

    private void initSongPlayer() {
        try {
            final int maxSounds = (int) this.maxSoundsSpinner.getValue();
            if (this.songPlayer == null || this.audioSystemComboBox.getSelectedItem() != this.audioSystemType || this.songPlayer.getAudioSystem().getMaxSounds() != maxSounds) {
                this.closeSongPlayerAndVisualizer();
                this.audioSystemType = (AudioSystemType) this.audioSystemComboBox.getSelectedItem();
                this.songPlayer = switch (this.audioSystemType) {
                    case AUDIO_MIXER -> new RealtimeSongRenderer(this.song, soundData -> new AudioMixerAudioSystem(soundData, maxSounds, true, false, PLAYBACK_AUDIO_FORMAT));
                    case OPENAL -> new RealtimeSongPlayer(this.song, soundData -> new OpenALAudioSystem(soundData, maxSounds));
                    case BASS -> new RealtimeSongPlayer(this.song, soundData -> new BassAudioSystem(soundData, maxSounds));
                    case AUDIO_MIXER_MULTITHREADED -> new RealtimeSongRenderer(this.song, soundData -> new AudioMixerAudioSystem(soundData, maxSounds, true, true, PLAYBACK_AUDIO_FORMAT));
                    case X_AUDIO_2 -> new RealtimeSongPlayer(this.song, soundData -> new XAudio2AudioSystem(soundData, maxSounds));
                };
                this.songPlayer.getAudioSystem().setMasterVolume(this.volumeSlider.getValue() / 100F);
            }
        } catch (Throwable t) {
            this.audioSystemType = null;
            this.songPlayer = null;
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, AUDIO_SYSTEM_UNAVAILABLE_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
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
        this.openVisualizerButton.setEnabled(this.songPlayer != null);
        if (this.songPlayer != null && this.songPlayer.isRunning()) {
            this.audioSystemComboBox.setEnabled(false);
            this.maxSoundsSpinner.setEnabled(false);
            this.pauseResumeButton.setEnabled(true);
            this.progressSlider.setEnabled(true);
            this.playStopButton.setText("Stop");
            this.pauseResumeButton.setText(this.songPlayer.isPaused() ? "Resume" : "Pause");

            final int tickCount = this.songPlayer.getSong().getNotes().getLengthInTicks();
            if (this.progressSlider.getMaximum() != tickCount) {
                this.progressSlider.setMaximum(tickCount);
            }
            this.progressSlider.setValue(this.songPlayer.getTick());

            final int seconds = (int) Math.ceil(this.songPlayer.getMillisecondPosition() / 1000F);
            this.progressLabel.setText("Current Position: " + String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60));
            this.statusLine.setText(String.join(", ", this.songPlayer.getStatusLines()));
        } else {
            this.audioSystemComboBox.setEnabled(true);
            this.maxSoundsSpinner.setEnabled(true);
            this.pauseResumeButton.setEnabled(false);
            this.progressSlider.setEnabled(false);
            this.playStopButton.setText("Play");
            this.pauseResumeButton.setText("Pause");
            this.progressSlider.setValue(0);
            this.progressLabel.setText(" ");
            this.statusLine.setText(" ");
        }
    }

    private void closeSongPlayerAndVisualizer() {
        if (this.visualizerWindow != null) {
            this.visualizerWindow.close();
            this.visualizerWindow = null;
        }
        if (this.songPlayer != null) {
            this.songPlayer.close();
            this.songPlayer = null;
        }
    }


    private enum AudioSystemType {
        AUDIO_MIXER("AudioMixer"),
        OPENAL("OpenAL"),
        BASS("Un4seen BASS"),
        AUDIO_MIXER_MULTITHREADED("AudioMixer multithreaded (experimental)"),
        X_AUDIO_2("XAudio2 (Windows 10+ only)");

        private final String name;

        AudioSystemType(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}
