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

import net.raphimc.audiomixer.util.GrowableArray;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.player.FullNoteConsumer;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.audio.SoundMap;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public abstract class AudioExporter implements FullNoteConsumer {

    private final SongView<?> songView;
    protected final AudioFormat format;
    private final float masterVolume;
    private final Consumer<Float> progressConsumer;
    protected GrowableArray samples;
    private final long noteCount;
    protected final int samplesPerTick;
    private int processedNotes;

    public AudioExporter(final SongView<?> songView, final AudioFormat format, final float masterVolume, final Consumer<Float> progressConsumer) {
        this.songView = songView;
        this.format = format;
        this.progressConsumer = progressConsumer;
        this.masterVolume = masterVolume;

        this.noteCount = SongUtil.getNoteCount(songView);
        this.samplesPerTick = (int) Math.ceil(format.getSampleRate() / songView.getSpeed());
        this.samples = new GrowableArray(this.samplesPerTick * format.getChannels() * (songView.getLength() + Math.round(this.songView.getSpeed() * 3)));
    }

    public void render() throws InterruptedException {
        for (int tick = 0; tick < this.songView.getLength(); tick++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            final List<? extends Note> notes = this.songView.getNotesAtTick(tick);
            this.preTick();
            for (Note note : notes) this.playNote(note);
            this.postTick();
            this.processedNotes += notes.size();

            this.progressConsumer.accept((float) this.processedNotes / this.noteCount);
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        }

        final int threeSeconds = Math.round(this.songView.getSpeed() * 3);
        for (int i = 0; i < threeSeconds; i++) {
            this.postTick();
        }

        this.finish();
    }

    public int[] getSamples() {
        return this.samples.getArray();
    }

    @Override
    public void playNote(final Instrument instrument, final float pitch, final float volume, final float panning) {
        this.processSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), pitch, volume * this.masterVolume, panning);
    }

    @Override
    public void playCustomNote(final NbsCustomInstrument customInstrument, final float pitch, final float volume, final float panning) {
        this.processSound(customInstrument.getSoundFileName().replace(File.separatorChar, '/'), pitch, volume * this.masterVolume, panning);
    }

    protected abstract void processSound(final String sound, final float pitch, final float volume, final float panning);

    protected void preTick() {
    }

    protected abstract void postTick();

    protected abstract void finish();

}
