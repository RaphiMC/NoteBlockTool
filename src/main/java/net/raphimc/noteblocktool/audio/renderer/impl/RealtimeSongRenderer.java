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
package net.raphimc.noteblocktool.audio.renderer.impl;

import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.audiomixer.util.SourceDataLineWriter;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.renderer.SongRenderer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.util.List;

public class RealtimeSongRenderer extends SongRenderer {

    private final SourceDataLineWriter sourceDataLineWriter;

    public RealtimeSongRenderer(final Song song, final int maxSounds, final boolean normalized, final boolean threaded, final PcmFloatAudioFormat audioFormat) {
        super(song, maxSounds, normalized, threaded, audioFormat);
        try {
            final AudioFormat playbackAudioFormat = new AudioFormat(audioFormat.getSampleRate(), Short.SIZE, audioFormat.getChannels(), true, false);
            this.sourceDataLineWriter = new SourceDataLineWriter(AudioSystem.getSourceDataLine(playbackAudioFormat), 50, this::renderTick);
            this.sourceDataLineWriter.start();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to open SourceDataLine", e);
        }
    }

    @Override
    public List<String> getStatusLines() {
        final List<String> statusLines = super.getStatusLines();
        statusLines.add("Audio Renderer CPU Load: " + (int) this.sourceDataLineWriter.getCpuLoad() + "%");
        return statusLines;
    }

    @Override
    public void close() {
        this.sourceDataLineWriter.close();
        super.close();
    }

}
