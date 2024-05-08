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
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.audio.export.AudioMerger;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JavaxAudioExporter extends AudioExporter {

    private final Map<Instrument, int[]> sounds;
    private final Map<String, int[]> mutationCache;
    private final AudioMerger merger;

    public JavaxAudioExporter(final SongView<?> songView, final AudioFormat format, final Consumer<Float> progressConsumer) {
        super(songView, format, progressConsumer);
        this.sounds = SoundMap.loadInstrumentSamples(format);
        this.mutationCache = new HashMap<>();
        this.merger = new AudioMerger(this.samplesPerTick * (songView.getLength() + 1));
    }

    @Override
    protected void processNote(final Instrument instrument, final float volume, final float pitch, final float panning) {
        String key = instrument + "\0" + volume + "\0" + pitch;
        this.merger.addSamples(this.mutationCache.computeIfAbsent(key, k -> SoundSampleUtil.mutate(this.sounds.get(instrument), volume, pitch)));
    }

    @Override
    protected void writeSamples() {
        this.merger.pushSamples(this.samplesPerTick);
    }

    @Override
    protected void finish() {
        switch (this.format.getSampleSizeInBits()) {
            case 8:
                for (byte b : this.merger.normalizeBytes()) {
                    this.sampleOutputStream.writeSample(b);
                }
                break;
            case 16:
                for (short s : this.merger.normalizeShorts()) {
                    this.sampleOutputStream.writeSample(s);
                }
                break;
            case 32:
                for (int i : this.merger.normalizeInts()) {
                    this.sampleOutputStream.writeSample(i);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported sample size: " + this.format.getSampleSizeInBits());
        }
    }

}
