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
package net.raphimc.noteblocktool.audio.export;

import net.raphimc.noteblocklib.format.nbs.NbsDefinitions;
import net.raphimc.noteblocklib.format.nbs.model.NbsNote;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.NoteWithPanning;
import net.raphimc.noteblocklib.model.NoteWithVolume;
import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocklib.util.MinecraftDefinitions;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public abstract class AudioExporter {

    private final SongView<?> songView;
    protected final AudioFormat format;
    private final Consumer<Float> progressConsumer;
    protected SampleOutputStream sampleOutputStream;
    private final long noteCount;
    protected final int samplesPerTick;
    private int processedNotes;

    public AudioExporter(final SongView<?> songView, final AudioFormat format, final Consumer<Float> progressConsumer) {
        this.songView = songView;
        this.format = format;
        this.progressConsumer = progressConsumer;
        this.sampleOutputStream = new SampleOutputStream(format);

        this.noteCount = songView.getNotes().values().stream().mapToLong(List::size).sum();
        this.samplesPerTick = (int) (format.getSampleRate() / songView.getSpeed());
    }

    public void render() throws InterruptedException {
        for (int tick = 0; tick <= this.songView.getLength(); tick++) {
            List<? extends Note> notes = this.songView.getNotesAtTick(tick);
            this.processNotes(notes);
            this.writeSamples();

            this.progressConsumer.accept((float) this.processedNotes / this.noteCount);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        }
        this.finish();
    }

    public void write(final File file) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(this.sampleOutputStream.getBytes());
        AudioInputStream audioInputStream = new AudioInputStream(bais, this.format, bais.available());
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
        audioInputStream.close();
    }

    private void processNotes(final List<? extends Note> notes) throws InterruptedException {
        for (Note note : notes) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (note.getInstrument() >= Instrument.values().length) continue;
            final float volume;
            if (note instanceof NoteWithVolume) {
                final NoteWithVolume noteWithVolume = (NoteWithVolume) note;
                volume = noteWithVolume.getVolume();
            } else {
                volume = 100F;
            }
            if (volume <= 0) continue;
            final float panning;
            if (note instanceof NoteWithPanning) {
                final NoteWithPanning noteWithPanning = (NoteWithPanning) note;
                panning = noteWithPanning.getPanning();
            } else {
                panning = 0F;
            }
            final float pitch;
            if (note instanceof NbsNote) {
                final NbsNote nbsNote = (NbsNote) note;
                pitch = MinecraftDefinitions.nbsPitchToMcPitch(NbsDefinitions.getPitch(nbsNote));
            } else {
                pitch = MinecraftDefinitions.mcKeyToMcPitch(MinecraftDefinitions.nbsKeyToMcKey(note.getKey()));
            }
            final Instrument instrument = Instrument.fromNbsId(note.getInstrument());
            final float playerVolume = volume / 100F;
            final float playerPanning = panning / 100F;

            this.processNote(instrument, playerVolume, pitch, playerPanning);
        }
        this.processedNotes += notes.size();
    }

    protected abstract void processNote(final Instrument instrument, final float volume, final float pitch, final float panning);

    protected abstract void writeSamples();

    protected abstract void finish();

}
