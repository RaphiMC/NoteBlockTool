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
package net.raphimc.noteblocktool.audio.export.impl;

import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.audio.soundsystem.impl.OpenALSoundSystem;

import javax.sound.sampled.AudioFormat;
import java.util.function.Consumer;

public class OpenALAudioExporter extends AudioExporter {

    private final OpenALSoundSystem soundSystem;

    public OpenALAudioExporter(final OpenALSoundSystem soundSystem, final SongView<?> songView, final AudioFormat format, final Consumer<Float> progressConsumer) {
        super(songView, format, progressConsumer);
        this.soundSystem = soundSystem;
    }

    @Override
    protected void processNote(final Instrument instrument, final float volume, final float pitch, final float panning) {
        this.soundSystem.playNote(instrument, volume, pitch, panning);
    }

    @Override
    protected void writeSamples() {
        this.soundSystem.renderSamples(this.sampleOutputStream, this.samplesPerTick);
    }

    @Override
    protected void finish() {
        this.soundSystem.stopSounds();
    }

}