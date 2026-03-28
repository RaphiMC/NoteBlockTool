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

import net.raphimc.audiomixer.NormalizedAudioMixer;
import net.raphimc.audiomixer.io.AudioIO;
import net.raphimc.audiomixer.pcmsource.impl.MonoStaticPcmSource;
import net.raphimc.audiomixer.sound.impl.mix.MixSound;
import net.raphimc.audiomixer.sound.impl.mix.ThreadedChannelMixSound;
import net.raphimc.audiomixer.sound.impl.pcm.OptimizedMonoSound;
import net.raphimc.audiomixer.util.GrowableArray;
import net.raphimc.audiomixer.util.MathUtil;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.util.SoundFileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public abstract class SongRenderer extends SongPlayer implements AutoCloseable {

    private final Map<String, float[]> sounds = new HashMap<>();
    private final NormalizedAudioMixer audioMixer;
    private final MixSound masterMixSound;
    private boolean running;
    private boolean timingJitter;
    private long lastTickTime;

    public SongRenderer(final Song song, final int maxSounds, final boolean normalized, final boolean threaded, final PcmFloatAudioFormat audioFormat) {
        super(song);
        this.setCustomScheduler(null);
        try {
            for (Map.Entry<String, byte[]> entry : SoundMap.loadSoundData(song).entrySet()) {
                this.sounds.put(entry.getKey(), AudioIO.readSamples(SoundFileUtil.readAudioFile(new ByteArrayInputStream(entry.getValue())), new PcmFloatAudioFormat(audioFormat.getSampleRate(), 1)));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
        this.audioMixer = new NormalizedAudioMixer(audioFormat);
        if (!normalized) {
            this.audioMixer.getSoundModifiers().remove(this.audioMixer.getNormalizationModifier());
        }
        if (threaded) {
            this.masterMixSound = new ThreadedChannelMixSound(Math.max(1, Runtime.getRuntime().availableProcessors() - 2));
            this.audioMixer.playSound(this.masterMixSound);
        } else {
            this.masterMixSound = this.audioMixer.getMasterMixSound();
        }
        this.masterMixSound.setMaxSounds(maxSounds);
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        for (Note note : notes) {
            if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                this.playSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), note.getPitch(), note.getVolume(), note.getPanning());
            } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                this.playSound(instrument.getSoundFilePathOr("").replace(File.separatorChar, '/'), note.getPitch(), note.getVolume(), note.getPanning());
            } else {
                throw new IllegalArgumentException("Unsupported instrument class: " + note.getInstrument().getClass().getName());
            }
        }
    }

    private void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (volume <= 0F) {
            return;
        }
        if (!this.sounds.containsKey(sound)) {
            return;
        }
        this.masterMixSound.playSound(new OptimizedMonoSound(new MonoStaticPcmSource(this.sounds.get(sound)), pitch, volume, panning));
    }

    public float[] renderTick() {
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

    public float[] renderSong() throws InterruptedException {
        final GrowableArray samples = new GrowableArray(MathUtil.millisToSampleCount(this.audioMixer.getAudioFormat(), (this.getSong().getLengthInSeconds() + 5) * 1000F));
        this.start();
        while (this.isRunning()) {
            samples.add(this.renderTick());
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
        samples.add(this.audioMixer.renderMillis(3000F));
        return samples.getArray();
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

    public void setMasterVolume(final float volume) {
        this.audioMixer.setMasterVolume(volume);
    }

    public void stopAllSounds() {
        this.masterMixSound.stopAllSounds();
    }

    public void setTimingJitter(final boolean timingJitter) {
        this.timingJitter = timingJitter;
    }

    public List<String> getStatusLines() {
        final List<String> statusLines = new ArrayList<>();
        statusLines.add("Sounds: " + this.masterMixSound.getMixedSounds() + " / " + this.masterMixSound.getMaxSounds());
        return statusLines;
    }

    @Override
    public void close() {
        this.stop();
        if (this.masterMixSound instanceof ThreadedChannelMixSound threadedMixSound) {
            threadedMixSound.close();
        }
    }

}
