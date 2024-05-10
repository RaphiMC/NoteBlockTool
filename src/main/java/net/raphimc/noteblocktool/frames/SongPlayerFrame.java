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
import net.raphimc.noteblocklib.format.nbs.NbsSong;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsNote;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.player.FullNoteConsumer;
import net.raphimc.noteblocklib.player.ISongPlayerCallback;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocklib.util.SongResampler;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.audio.soundsystem.impl.JavaxSoundSystem;
import net.raphimc.noteblocktool.audio.soundsystem.impl.OpenALSoundSystem;
import net.raphimc.noteblocktool.elements.FastScrollPane;
import net.raphimc.noteblocktool.elements.NewLineLabel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Optional;

public class SongPlayerFrame extends JFrame implements ISongPlayerCallback, FullNoteConsumer {

    private static final String UNAVAILABLE_MESSAGE = "An error occurred while initializing the sound system.\nPlease make sure that your system supports the selected sound system.";
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static SongPlayerFrame instance;
    private static Point lastPosition;
    private static int lastMaxSounds = 256;
    private static int lastVolume = 50;

    public static void open(final ListFrame.LoadedSong song) {
        open(song, song.getSong().getView().clone());
    }

    public static void open(final ListFrame.LoadedSong song, final SongView<?> view) {
        if (instance != null && instance.isVisible()) {
            lastPosition = instance.getLocation();
            lastMaxSounds = (int) instance.maxSoundsSpinner.getValue();
            lastVolume = instance.volumeSlider.getValue();
            instance.dispose();
        }
        instance = new SongPlayerFrame(song, view);
        if (lastPosition != null) instance.setLocation(lastPosition);
        instance.maxSoundsSpinner.setValue(lastMaxSounds);
        instance.volumeSlider.setValue(lastVolume);
        instance.playStopButton.doClick();
        instance.setVisible(true);
    }

    public static void close() {
        if (instance != null) instance.dispose();
    }


    private final ListFrame.LoadedSong song;
    private final SongPlayer songPlayer;
    private final Timer updateTimer;
    private final JComboBox<String> soundSystemComboBox = new JComboBox<>(new String[]{"OpenAL (better sound quality)", "Javax (better system compatibility, laggier)"});
    private final JSpinner maxSoundsSpinner = new JSpinner(new SpinnerNumberModel(256, 64, 8192, 64));
    private final JSlider volumeSlider = new JSlider(0, 100, 50);
    private final JButton playStopButton = new JButton("Play");
    private final JButton pauseResumeButton = new JButton("Pause");
    private final JSlider progressSlider = new JSlider(0, 100, 0);
    private final JLabel soundCount = new JLabel("Sounds: 0/" + DECIMAL_FORMAT.format(this.maxSoundsSpinner.getValue()));
    private final JLabel progressLabel = new JLabel("Current Position: 00:00:00");
    private SoundSystem soundSystem;

    private SongPlayerFrame(final ListFrame.LoadedSong song, final SongView<?> view) {
        this.song = song;
        this.songPlayer = new SongPlayer(this.getSongView(view), this);
        this.updateTimer = new Timer(50, e -> this.tick());
        this.updateTimer.start();

        this.setTitle("NoteBlockTool Song Player - " + this.songPlayer.getSongView().getTitle());
        this.setIconImage(new ImageIcon(this.getClass().getResource("/icon.png")).getImage());
        this.setSize(500, 400);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);

        this.initComponents();
        this.initFrameHandler();

        this.setMinimumSize(this.getSize());
    }

    private SongView<?> getSongView(final SongView<?> view) {
        if (this.song.getSong() instanceof NbsSong) {
            SongResampler.applyNbsTempoChangers(((NbsSong) this.song.getSong()), (SongView<NbsNote>) view);
        }
        return view;
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
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.soundSystemComboBox);

            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.LINE_START).add(new JLabel("Max Sounds:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.maxSoundsSpinner, () -> {
                this.maxSoundsSpinner.addChangeListener(e -> {
                    lastMaxSounds = (int) this.maxSoundsSpinner.getValue();
                });
            });

            GBC.create(northPanel).grid(0, gridy).insets(5, 5, 5, 5).anchor(GBC.LINE_START).add(new JLabel("Volume:"));
            GBC.create(northPanel).grid(1, gridy++).insets(5, 0, 5, 5).weightx(1).fill(GBC.HORIZONTAL).add(this.volumeSlider, () -> {
                this.volumeSlider.setPaintLabels(true);
                this.volumeSlider.setPaintTicks(true);
                this.volumeSlider.setMajorTickSpacing(25);
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
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.songPlayer.getSongView().getTitle()));

            Optional<String> author = this.song.getAuthor();
            if (author.isPresent()) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Author:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(author.get()));
            }

            Optional<String> originalAuthor = this.song.getOriginalAuthor();
            if (originalAuthor.isPresent()) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Original Author:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(originalAuthor.get()));
            }

            Optional<String> description = this.song.getDescription();
            if (description.isPresent()) {
                GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Description:"));
                GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(description.get()));
            }

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Length:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(this.song.getLength(this.songPlayer.getSongView())));

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Note count:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(DECIMAL_FORMAT.format(this.song.getNoteCount(this.songPlayer.getSongView()))));

            GBC.create(centerPanel).grid(0, gridy).insets(5, 5, 0, 5).anchor(GBC.NORTHWEST).add(new JLabel("Speed:"));
            GBC.create(centerPanel).grid(1, gridy++).insets(5, 0, 0, 5).weightx(1).fill(GBC.HORIZONTAL).add(new NewLineLabel(DECIMAL_FORMAT.format(this.songPlayer.getSongView().getSpeed())));

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
                        this.songPlayer.play();
                        this.songPlayer.setPaused(true);
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
                    if (this.soundSystem != null) this.soundSystem.stopSounds();
                } else {
                    if (this.initSoundSystem()) {
                        this.soundSystem.setMasterVolume(this.volumeSlider.getValue() / 100F);
                        this.songPlayer.play();
                    }
                }
            });
            buttonPanel.add(this.pauseResumeButton);
            this.pauseResumeButton.addActionListener(e -> this.songPlayer.setPaused(!this.songPlayer.isPaused()));
            GBC.create(southPanel).grid(0, gridy++).insets(5, 5, 5, 5).weightx(1).width(2).fill(GBC.HORIZONTAL).add(buttonPanel);

            final JPanel statusBar = new JPanel();
            statusBar.setBorder(BorderFactory.createEtchedBorder());
            statusBar.setLayout(new GridLayout(1, 1));
            statusBar.add(this.soundCount);
            GBC.create(southPanel).grid(0, gridy++).weightx(1).fill(GBC.HORIZONTAL).add(statusBar);
        }
    }

    private boolean initSoundSystem() {
        int currentIndex = -1;
        if (this.soundSystem instanceof OpenALSoundSystem) currentIndex = 0;
        else if (this.soundSystem instanceof JavaxSoundSystem) currentIndex = 1;

        try {
            if (this.soundSystem == null || this.soundSystemComboBox.getSelectedIndex() != currentIndex || this.soundSystem.getMaxSounds() != (int) this.maxSoundsSpinner.getValue()) {
                if (this.soundSystem != null) this.soundSystem.close();

                if (this.soundSystemComboBox.getSelectedIndex() == 0) {
                    this.soundSystem = OpenALSoundSystem.createPlayback(((Number) this.maxSoundsSpinner.getValue()).intValue());
                } else if (this.soundSystemComboBox.getSelectedIndex() == 1) {
                    this.soundSystem = new JavaxSoundSystem(this.songPlayer.getSongView().getSpeed());
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
                SongPlayerFrame.this.soundSystem.close();
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

            int tickCount = this.songPlayer.getSongView().getLength();
            if (this.progressSlider.getMaximum() != tickCount) this.progressSlider.setMaximum(tickCount);
            this.progressSlider.setValue(this.songPlayer.getTick());
        } else {
            this.soundSystemComboBox.setEnabled(true);
            this.maxSoundsSpinner.setEnabled(true);
            this.playStopButton.setText("Play");
            this.pauseResumeButton.setText("Pause");
            this.pauseResumeButton.setEnabled(false);
            this.progressSlider.setValue(0);
        }
        this.soundCount.setText("Sounds: " + DECIMAL_FORMAT.format(this.soundSystem.getSoundCount()) + "/" + DECIMAL_FORMAT.format(this.soundSystem.getMaxSounds()));

        int msLength = (int) (this.songPlayer.getTick() / this.songPlayer.getSongView().getSpeed());
        this.progressLabel.setText("Current Position: " + String.format("%02d:%02d:%02d", msLength / 3600, (msLength / 60) % 60, msLength % 60));
    }

    @Override
    public void playNote(Instrument instrument, float pitch, float volume, float panning) {
        this.soundSystem.playSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), pitch, volume, panning);
    }

    @Override
    public void playCustomNote(NbsCustomInstrument customInstrument, float pitch, float volume, float panning) {
        this.soundSystem.playSound(customInstrument.getSoundFileName().replace(File.separatorChar, '/'), pitch, volume, panning);
    }

    @Override
    public void playNotes(java.util.List<? extends Note> notes) {
        for (Note note : notes) this.playNote(note);

        this.soundSystem.writeSamples();
    }

}
