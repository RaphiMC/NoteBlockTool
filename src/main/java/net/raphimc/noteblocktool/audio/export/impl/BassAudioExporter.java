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
package net.raphimc.noteblocktool.audio.export.impl;

import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.audio.soundsystem.impl.BassSoundSystem;

import java.util.function.Consumer;

public class BassAudioExporter extends AudioExporter {

    private final BassSoundSystem soundSystem;

    public BassAudioExporter(final Song song, final PcmFloatAudioFormat audioFormat, final float masterVolume, final Consumer<Float> progressConsumer) {
        super(song, audioFormat, masterVolume, progressConsumer);
        this.soundSystem = BassSoundSystem.createCapture(SoundMap.loadSoundData(song), 8192, audioFormat);
    }

    @Override
    public void render() throws InterruptedException {
        try {
            super.render();
        } finally {
            this.soundSystem.close();
        }
    }

    @Override
    protected void processSound(final String sound, final float pitch, final float volume, final float panning) {
        this.soundSystem.playSound(sound, pitch, volume, panning);
    }

    @Override
    protected boolean preTick() {
        this.soundSystem.preTick();
        return super.preTick();
    }

    @Override
    protected void mix(final int framesPerTick) {
        this.samples.add(this.soundSystem.renderSamples(framesPerTick));
    }

}
