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
package net.raphimc.noteblocktool.audio.renderer;

import net.raphimc.audiomixer.LimitingAudioMixer;
import net.raphimc.audiomixer.automation.finite.FiniteAutomation;
import net.raphimc.audiomixer.automation.finite.ramp.impl.LinearRampAutomation;
import net.raphimc.audiomixer.io.AudioIo;
import net.raphimc.audiomixer.mixer.Mixer;
import net.raphimc.audiomixer.mixer.MultithreadedMixer;
import net.raphimc.audiomixer.processor.dynamics.GainProcessor;
import net.raphimc.audiomixer.processor.spatial.GainPanProcessor;
import net.raphimc.audiomixer.processor.spatial.PanProcessor;
import net.raphimc.audiomixer.source.audio.impl.BufferedAudioSource;
import net.raphimc.audiomixer.util.AudioFormat;
import net.raphimc.audiomixer.util.buffer.AudioBuffer;
import net.raphimc.audiomixer.util.buffer.AudioBufferBuilder;
import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.format.nbs.model.event.NbsSoundStopperEvent;
import net.raphimc.noteblocklib.model.event.Event;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocktool.audio.SoundMap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class SongRenderer extends SongPlayer implements AutoCloseable {

    private final Map<String, AudioBuffer> sounds = new HashMap<>();
    private final LimitingAudioMixer audioMixer;
    private final Mixer masterMixer;
    private final int maxSourceCount;
    private boolean running;
    private boolean timingJitter;
    private long lastTickTime;

    public SongRenderer(final Song song, final int maxSounds, final boolean limited, final boolean threaded, final AudioFormat audioFormat) {
        super(song);
        this.setCustomScheduler(null);
        try {
            for (Map.Entry<String, byte[]> entry : SoundMap.loadSoundData(song).entrySet()) {
                this.sounds.put(entry.getKey(), AudioIo.read(new ByteArrayInputStream(entry.getValue()), audioFormat.withChannels(1)));
            }
        } catch (final Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
        this.audioMixer = new LimitingAudioMixer(audioFormat);
        this.audioMixer.getLimiterProcessor().setEnabled(limited);
        if (threaded) {
            this.masterMixer = new MultithreadedMixer();
            this.audioMixer.add(this.masterMixer);
        } else {
            this.masterMixer = this.audioMixer;
        }
        this.maxSourceCount = maxSounds;
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        for (Note note : notes) {
            final String sound;
            if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                sound = SoundMap.INSTRUMENT_SOUNDS.get(instrument);
            } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                sound = instrument.getSoundFilePathOr("").replace(File.separatorChar, '/');
            } else {
                throw new IllegalArgumentException("Unsupported instrument class: " + note.getInstrument().getClass().getName());
            }
            if (note.getVolume() > 0F && this.sounds.containsKey(sound)) {
                this.masterMixer.add(new NoteAudioSource(this.sounds.get(sound), note));
            }
        }
        this.masterMixer.limitSourceCount(this.maxSourceCount);
    }

    @Override
    protected void handleEvents(final List<Event> events) {
        for (Event event : events) {
            if (event instanceof NbsSoundStopperEvent soundStopperEvent) {
                this.masterMixer.forEach(source -> {
                    if (source instanceof NoteAudioSource noteAudioSource && soundStopperEvent.shouldStop(noteAudioSource.note)) {
                        final GainProcessor gainProcessor = new GainProcessor();
                        source.processors().add(gainProcessor);
                        final FiniteAutomation automation = new LinearRampAutomation(gainProcessor.gain(), 0F, 100F);
                        automation.finishListeners().add(ignored1 -> this.audioMixer.preRenderActions().add(ignored2 -> this.masterMixer.remove(source)));
                        source.automations().add(automation);
                    }
                });
            }
        }
    }

    public AudioBuffer renderTick() {
        if (this.isRunning()) {
            this.tick();
        }
        float millis = 1000F / this.getCurrentTicksPerSecond();
        if (this.timingJitter) {
            millis += ThreadLocalRandom.current().nextFloat(-1F, 1F);
            if (millis <= 0F) {
                millis = 0.1F;
            }
        }
        return this.audioMixer.renderMillis(millis);
    }

    public AudioBuffer renderSong() throws InterruptedException {
        final int expectedSampleCount = this.audioMixer.getFormat().millisToSampleCount((this.getSong().getLengthInSeconds() + 1) * 1000F);
        final AudioBufferBuilder bufferBuilder = new AudioBufferBuilder(this.audioMixer.getFormat(), expectedSampleCount);
        this.start();
        while (this.isRunning()) {
            bufferBuilder.append(this.renderTick());
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
        bufferBuilder.append(this.audioMixer.renderMillis(750F));
        return bufferBuilder.build();
    }

    @Override
    public void start(final int delay, final int tick) {
        super.start(delay, tick);
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        super.stop();
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public void setTick(final int tick) {
        super.setTick(tick);
        this.lastTickTime = System.nanoTime();
    }

    @Override
    protected void tick() {
        super.tick();
        this.lastTickTime = System.nanoTime();
    }

    public long getLastTickTime() {
        return this.lastTickTime;
    }

    @Override
    public void setPaused(final boolean paused) {
        super.setPaused(paused);
        if (paused) {
            this.stopAllSounds();
        }
    }

    public void setMasterVolume(final int volume) {
        this.audioMixer.gain().set(volume / 100F);
    }

    public void stopAllSounds() {
        this.masterMixer.clear();
    }

    public void setTimingJitter(final boolean timingJitter) {
        this.timingJitter = timingJitter;
    }

    public List<String> getStatusLines() {
        final List<String> statusLines = new ArrayList<>();
        statusLines.add("Sounds: " + this.masterMixer.getMixedSourceCount() + " / " + this.maxSourceCount);
        return statusLines;
    }

    @Override
    public void close() {
        this.stop();
    }

    private static final class NoteAudioSource extends BufferedAudioSource {

        private final Note note;

        private NoteAudioSource(final AudioBuffer buffer, final Note note) {
            super(buffer);
            this.note = note;
            this.pitch().set(note.getPitch());
            if (note.getPanning() != 0F && note.getVolume() != 1F) {
                this.processors().add(new GainPanProcessor(note.getVolume(), note.getPanning()));
            } else if (note.getVolume() != 1F) {
                this.processors().add(new GainProcessor(note.getVolume()));
            } else if (note.getPanning() != 0F) {
                this.processors().add(new PanProcessor(note.getPanning()));
            }
        }

    }

}
