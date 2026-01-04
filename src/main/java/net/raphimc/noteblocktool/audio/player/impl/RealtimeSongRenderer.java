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
package net.raphimc.noteblocktool.audio.player.impl;

import net.raphimc.audiomixer.util.SourceDataLineWriter;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.system.AudioSystem;

import javax.sound.sampled.AudioFormat;
import java.util.Map;
import java.util.function.Function;

public class RealtimeSongRenderer extends SongRenderer {

    private final SourceDataLineWriter sourceDataLineWriter;

    public RealtimeSongRenderer(final Song song, final Function<Map<String, byte[]>, AudioSystem> audioSystemSupplier) {
        super(song, audioSystemSupplier);
        try {
            final AudioFormat audioFormat = new AudioFormat(this.getAudioSystem().getLoopbackAudioFormat().getSampleRate(), Short.SIZE, this.getAudioSystem().getLoopbackAudioFormat().getChannels(), true, false);
            this.sourceDataLineWriter = new SourceDataLineWriter(javax.sound.sampled.AudioSystem.getSourceDataLine(audioFormat), 50, this::renderTick);
            this.sourceDataLineWriter.start();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to open SourceDataLine", e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.getAudioSystem().stopAllSounds();
    }

    @Override
    public void setPaused(final boolean paused) {
        super.setPaused(paused);
        if (paused) {
            this.getAudioSystem().stopAllSounds();
        }
    }

    @Override
    public void close() {
        this.sourceDataLineWriter.close();
        super.close();
    }

    @Override
    protected Float getAudioRendererCpuLoad() {
        return this.sourceDataLineWriter.getCpuLoad();
    }

}
