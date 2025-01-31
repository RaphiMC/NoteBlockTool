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
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.audio.soundsystem.impl.*;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.NewLineLabel;
import net.raphimc.noteblocktool.frames.visualizer.VisualizerWindow;
import net.raphimc.noteblocktool.util.SoundSystemSongPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.Map;

public class SongPlayerFrame extends JFrame {

    private static final String UNAVAILABLE_MESSAGE = "An error occurred while initializing the sound system.\nPlease make sure that your system supports the selected sound system.";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static SongPlayerFrame instance;
    private static Point lastPosition;
    private static int lastSoundSystem;
    private static int lastMaxSounds = 1024;
    private static int lastVolume = 50;

    public static void open(final Song song) {
        if (instance != null && instance.isVisible()) {
            lastPosition = instance.getLocation();
            lastSoundSystem = instance.soundSystemComboBox.getSelectedIndex();
            lastMaxSounds = (int) instance.maxSoundsSpinner.getValue();
            lastVolume = instance.volumeSlider.getValue();
            instance.dispose();
        }
        SwingUtilities.invokeLater(() -> {
            instance = new SongPlayerFrame(song);
            if (lastPosition != null) instance.setLocation(lastPosition);
            instance.soundSystemComboBox.setSelectedIndex(lastSoundSystem);
            instance.maxSoundsSpinner.setValue(lastMaxSounds);
            instance.volumeSlider.setValue(lastVolume);
            VisualizerWindow.getInstance().open(instance.songPlayer);
            instance.playStopButton.doClick(0);
            instance.setVisible(true);
        });
    }

    public static void close() {
        if (instance != null) instance.dispose();
    }


    private final Song song;
    private final SoundSystemSongPlayer songPlayer;
    private final Timer updateTimer;
    private final JComboBox<String> soundSystemComboBox = new JComboBox<>(new String[]{"AudioMixer", "OpenAL", "Un4seen BASS", "AudioMixer multithreaded (experimental)", "XAudio2 (Windows 10+ only)"});
    private final JSpinner maxSoundsSpinner = new JSpinner(new SpinnerNumberModel(lastMaxSounds, 64, 40960, 64));
    private final JSlider volumeSlider = new JSlider(0, 100, lastVolume);
    private final JButton playStopButton = new JButton("Play");
    private final JButton pauseResumeButton = new JButton("Pause");
    private final JSlider progressSlider = new JSlider(0, 100, 0);
    private final JLabel statusLine = new JLabel(" ");
    private final JLabel progressLabel = new JLabel("Current Position: 00:00:00");
    private SoundSystem soundSystem;

    private SongPlayerFrame(final Song song) {
        this.song = song;
        this.songPlayer = new SoundSystemSongPlayer(song);
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
            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Sound System:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.soundSystemComboBox, () -> {
                this.soundSystemComboBox.addActionListener(e -> lastSoundSystem = this.soundSystemComboBox.getSelectedIndex());
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
                    if (this.soundSystem != null) this.soundSystem.setMasterVolume(this.volumeSlider.getValue() / 100F);
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
                    //Skip updates if the value is set directly
                    if (!this.progressSlider.getValueIsAdjusting()) return;
                    if (!this.songPlayer.isRunning()) {
                        if (this.initSoundSystem()) {
                            this.soundSystem.setMasterVolume(this.volumeSlider.getValue() / 100F);
                            this.songPlayer.start(this.soundSystem);
                            this.songPlayer.setPaused(true);
                        }
                    }
                    this.songPlayer.setTick(this.progressSlider.getValue());
                });
            });

            final JPanel buttonPanel = new JPanel();
            buttonPanel.setLayout(new GridLayout(1, 3, 5, 0));
            buttonPanel.add(this.playStopButton);
            this.playStopButton.addActionListener(e -> {
                if (this.songPlayer.isRunning()) {
                    this.songPlayer.stop();
                    this.songPlayer.setTick(0);
                } else {
                    if (this.initSoundSystem()) {
                        this.soundSystem.setMasterVolume(this.volumeSlider.getValue() / 100F);
                        this.songPlayer.start(this.soundSystem);
                    }
                }
            });
            buttonPanel.add(this.pauseResumeButton);
            this.pauseResumeButton.addActionListener(e -> {
                if (this.songPlayer.isPaused()) {
                    this.songPlayer.setPaused(false);
                } else {
                    this.songPlayer.setPaused(true);
                    this.soundSystem.stopSounds();
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

    private boolean initSoundSystem() {
        final int currentIndex;
        if (this.soundSystem instanceof MultithreadedAudioMixerSoundSystem) {
            currentIndex = 3;
        } else if (this.soundSystem instanceof AudioMixerSoundSystem) {
            currentIndex = 0;
        } else if (this.soundSystem instanceof OpenALSoundSystem) {
            currentIndex = 1;
        } else if (this.soundSystem instanceof BassSoundSystem) {
            currentIndex = 2;
        } else if (this.soundSystem instanceof XAudio2SoundSystem) {
            currentIndex = 4;
        } else if (this.soundSystem == null) {
            currentIndex = -1;
        } else {
            throw new UnsupportedOperationException(UNAVAILABLE_MESSAGE);
        }

        try {
            if (this.soundSystem == null || this.soundSystemComboBox.getSelectedIndex() != currentIndex || this.soundSystem.getMaxSounds() != (int) this.maxSoundsSpinner.getValue()) {
                if (this.soundSystem != null) this.soundSystem.close();

                final Map<String, byte[]> soundData = SoundMap.loadSoundData(this.songPlayer.getSong());
                final int maxSounds = ((Number) this.maxSoundsSpinner.getValue()).intValue();

                if (this.soundSystemComboBox.getSelectedIndex() == 0) {
                    this.soundSystem = new AudioMixerSoundSystem(soundData, maxSounds);
                } else if (this.soundSystemComboBox.getSelectedIndex() == 1) {
                    this.soundSystem = OpenALSoundSystem.createPlayback(soundData, maxSounds);
                } else if (this.soundSystemComboBox.getSelectedIndex() == 2) {
                    this.soundSystem = BassSoundSystem.createPlayback(soundData, maxSounds);
                } else if (this.soundSystemComboBox.getSelectedIndex() == 3) {
                    this.soundSystem = new MultithreadedAudioMixerSoundSystem(soundData, maxSounds);
                } else if (this.soundSystemComboBox.getSelectedIndex() == 4) {
                    this.soundSystem = new XAudio2SoundSystem(soundData, maxSounds);
                } else {
                    throw new UnsupportedOperationException(UNAVAILABLE_MESSAGE);
                }
            }
            return this.soundSystem != null;
        } catch (Throwable t) {
            this.soundSystem = null;
            t.printStackTrace();
            JOptionPane.showMessageDialog(this, UNAVAILABLE_MESSAGE, "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
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
                SongPlayerFrame.this.songPlayer.stop();
                SongPlayerFrame.this.updateTimer.stop();
                VisualizerWindow.getInstance().hide();
                if (SongPlayerFrame.this.soundSystem != null) SongPlayerFrame.this.soundSystem.close();
            }
        });
    }

    private void tick() {
        if (this.songPlayer.isRunning()) {
            this.soundSystemComboBox.setEnabled(false);
            this.maxSoundsSpinner.setEnabled(false);
            this.playStopButton.setText("Stop");
            this.pauseResumeButton.setEnabled(true);
            if (this.songPlayer.isPaused()) this.pauseResumeButton.setText("Resume");
            else this.pauseResumeButton.setText("Pause");

            int tickCount = this.songPlayer.getSong().getNotes().getLengthInTicks();
            if (this.progressSlider.getMaximum() != tickCount) this.progressSlider.setMaximum(tickCount);
            this.progressSlider.setValue(this.songPlayer.getTick());
            this.statusLine.setText(this.soundSystem.getStatusLine() + ", Song Player CPU Load: " + (int) (this.songPlayer.getCpuLoad() * 100) + "%");
        } else {
            this.soundSystemComboBox.setEnabled(true);
            this.maxSoundsSpinner.setEnabled(true);
            this.playStopButton.setText("Play");
            this.pauseResumeButton.setText("Pause");
            this.pauseResumeButton.setEnabled(false);
            this.progressSlider.setValue(0);
            this.statusLine.setText(" ");
        }

        final int seconds = (int) Math.ceil(this.songPlayer.getMillisecondPosition() / 1000F);
        this.progressLabel.setText("Current Position: " + String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60));
    }

}
