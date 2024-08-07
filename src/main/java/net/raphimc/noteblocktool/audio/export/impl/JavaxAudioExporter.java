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
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.export.AudioBuffer;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class JavaxAudioExporter extends AudioExporter {

    private final Map<String, int[]> sounds;
    private final AudioBuffer merger;

    public JavaxAudioExporter(final SongView<?> songView, final AudioFormat format, final float masterVolume, final Consumer<Float> progressConsumer) {
        super(songView, format, masterVolume, progressConsumer);
        try {
            this.sounds = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : SoundMap.loadSoundData(songView).entrySet()) {
                this.sounds.put(entry.getKey(), SoundSampleUtil.readSamples(new ByteArrayInputStream(entry.getValue()), format));
            }
            this.merger = new AudioBuffer(this.samplesPerTick * format.getChannels() * songView.getLength());
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize javax audio exporter", e);
        }
    }

    @Override
    protected void processSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.merger.pushSamples(SoundSampleUtil.mutate(this.format, this.sounds.get(sound), pitch, volume, panning));
    }

    @Override
    protected void postTick() {
        this.merger.advanceIndex(this.samplesPerTick * this.format.getChannels());
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
