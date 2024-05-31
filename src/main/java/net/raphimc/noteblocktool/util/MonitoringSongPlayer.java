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
package net.raphimc.noteblocktool.util;

import net.raphimc.noteblocklib.model.SongView;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocklib.player.SongPlayerCallback;

public class MonitoringSongPlayer extends SongPlayer {

    private final long availableNanosPerTick;
    private long neededNanosPerTick = 0L;

    public MonitoringSongPlayer(final SongView<?> songView, final SongPlayerCallback callback) {
        super(songView, callback);

        this.availableNanosPerTick = (long) (1_000_000_000L / songView.getSpeed());
    }

    @Override
    protected void tick() {
        final long start = System.nanoTime();
        super.tick();
        this.neededNanosPerTick = System.nanoTime() - start;
    }

    public float getCpuLoad() {
        return (float) this.neededNanosPerTick / this.availableNanosPerTick;
    }

}
