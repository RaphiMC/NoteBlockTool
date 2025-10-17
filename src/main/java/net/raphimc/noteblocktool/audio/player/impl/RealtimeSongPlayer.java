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
package net.raphimc.noteblocktool.audio.player.impl;

import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.player.AudioSystemSongPlayer;
import net.raphimc.noteblocktool.audio.system.AudioSystem;

import java.util.Map;
import java.util.function.Function;

public class RealtimeSongPlayer extends AudioSystemSongPlayer {

    public RealtimeSongPlayer(final Song song, final Function<Map<String, byte[]>, AudioSystem> audioSystemSupplier) {
        super(song, audioSystemSupplier);
        if (this.getAudioSystem().getLoopbackAudioFormat() != null) {
            throw new IllegalArgumentException("AudioSystem isn't configured for realtime playback");
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

}
