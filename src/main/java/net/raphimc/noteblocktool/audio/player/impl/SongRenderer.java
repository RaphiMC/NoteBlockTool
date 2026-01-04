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

import net.raphimc.audiomixer.util.GrowableArray;
import net.raphimc.audiomixer.util.MathUtil;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocktool.audio.player.AudioSystemSongPlayer;
import net.raphimc.noteblocktool.audio.system.AudioSystem;

import java.util.Map;
import java.util.function.Function;

public class SongRenderer extends AudioSystemSongPlayer {

    private boolean isRunning;

    public SongRenderer(final Song song, final Function<Map<String, byte[]>, AudioSystem> audioSystemSupplier) {
        super(song, audioSystemSupplier);
        this.setCustomScheduler(null);
        if (this.getAudioSystem().getLoopbackAudioFormat() == null) {
            throw new IllegalArgumentException("AudioSystem isn't configured for loopback rendering");
        }
    }

    @Override
    public void start(final int delay, final int tick) {
        super.start(delay, tick);
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
        super.stop();
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    public float[] renderTick() {
        if (this.isRunning()) {
            this.tick();
        }
        return this.getAudioSystem().render(MathUtil.millisToFrameCount(this.getAudioSystem().getLoopbackAudioFormat(), 1000F / this.getCurrentTicksPerSecond()));
    }

    public float[] renderSong() throws InterruptedException {
        final GrowableArray samples = new GrowableArray(MathUtil.millisToSampleCount(this.getAudioSystem().getLoopbackAudioFormat(), (this.getSong().getLengthInSeconds() + 5) * 1000));
        this.start();
        while (this.isRunning()) {
            samples.add(this.renderTick());
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
        }
        samples.add(this.getAudioSystem().render(MathUtil.millisToFrameCount(this.getAudioSystem().getLoopbackAudioFormat(), 3000)));
        return samples.getArray();
    }

}
