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
package net.raphimc.noteblocktool.audio.export;

import net.raphimc.audiomixer.util.GrowableArray;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocktool.audio.SoundMap;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public abstract class AudioExporter extends SongPlayer {

    protected final AudioFormat format;
    private final float masterVolume;
    private final Consumer<Float> progressConsumer;
    protected GrowableArray samples;
    private final int noteCount;
    private int processedNotes;

    public AudioExporter(final Song song, final AudioFormat format, final float masterVolume, final Consumer<Float> progressConsumer) {
        super(song);
        this.format = format;
        this.progressConsumer = progressConsumer;
        this.masterVolume = masterVolume;

        this.noteCount = song.getNotes().getNoteCount();
        this.samples = new GrowableArray((song.getLengthInSeconds() + 5) * format.getChannels());
    }

    public void render() throws InterruptedException {
        this.start();
        while (this.isRunning()) {
            this.tick();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        }

        final int threeSeconds = Math.round(this.getCurrentTicksPerSecond() * 3);
        for (int i = 0; i < threeSeconds; i++) {
            this.postTick();
        }
    }

    public int[] getSamples() {
        return this.samples.getArray();
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        for (Note note : notes) {
            if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                this.processSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), note.getPitch(), note.getVolume() * this.masterVolume, note.getPanning());
            } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                this.processSound(instrument.getSoundFilePathOr("").replace(File.separatorChar, '/'), note.getPitch(), note.getVolume() * this.masterVolume, note.getPanning());
            } else {
                throw new IllegalArgumentException("Unsupported instrument type: " + note.getInstrument().getClass().getName());
            }
        }

        this.processedNotes += notes.size();
        this.progressConsumer.accept((float) this.processedNotes / this.noteCount);
    }

    @Override
    protected void postTick() {
        final int samplesPerTick = (int) Math.ceil(this.format.getSampleRate() / this.getCurrentTicksPerSecond());
        this.mix(samplesPerTick);
    }

    @Override
    protected void createTickTask() {
    }

    protected abstract void processSound(final String sound, final float pitch, final float volume, final float panning);

    protected abstract void mix(final int samplesPerTick);

}
