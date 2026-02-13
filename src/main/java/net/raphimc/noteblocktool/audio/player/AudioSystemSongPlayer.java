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
package net.raphimc.noteblocktool.audio.player;

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.system.AudioSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class AudioSystemSongPlayer extends SongPlayer implements AutoCloseable {

    private final AudioSystem audioSystem;
    private long lastTickTime;
    private float cpuLoad;

    public AudioSystemSongPlayer(final Song song, final Function<Map<String, byte[]>, AudioSystem> audioSystemSupplier) {
        super(song);
        this.audioSystem = audioSystemSupplier.apply(SoundMap.loadSoundData(song));
    }

    @Override
    public void setTick(final int tick) {
        super.setTick(tick);
        this.lastTickTime = System.nanoTime();
    }

    @Override
    protected void tick() {
        final long startTime = System.nanoTime();
        super.tick();
        this.lastTickTime = System.nanoTime();
        final float neededMillis = (this.lastTickTime - startTime) / 1_000_000F;
        final float availableMillis = 1000F / this.getCurrentTicksPerSecond();
        this.cpuLoad = (neededMillis / availableMillis) * 100F;
    }

    @Override
    protected boolean shouldTick() {
        this.audioSystem.preTick();
        return super.shouldTick();
    }

    @Override
    protected void playNotes(final List<Note> notes) {
        for (Note note : notes) {
            if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                this.audioSystem.playSound(SoundMap.INSTRUMENT_SOUNDS.get(instrument), note.getPitch(), note.getVolume(), note.getPanning());
            } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                this.audioSystem.playSound(instrument.getSoundFilePathOr("").replace(File.separatorChar, '/'), note.getPitch(), note.getVolume(), note.getPanning());
            } else {
                throw new IllegalArgumentException("Unsupported instrument type: " + note.getInstrument().getClass().getName());
            }
        }
    }

    @Override
    public void close() {
        this.stop();
        this.audioSystem.close();
    }

    public AudioSystem getAudioSystem() {
        return this.audioSystem;
    }

    public long getLastTickTime() {
        return this.lastTickTime;
    }

    public float getCpuLoad() {
        return this.cpuLoad;
    }

    public List<String> getStatusLines() {
        final List<String> statusLines = new ArrayList<>();
        statusLines.add("Sounds: " + this.audioSystem.getPlayingSounds() + " / " + this.audioSystem.getMaxSounds());
        if (this.getAudioRendererCpuLoad() != null) {
            statusLines.add("Audio Renderer CPU Load: " + this.getAudioRendererCpuLoad().intValue() + "%");
        }
        statusLines.add("Song Player CPU Load: " + (int) this.getCpuLoad() + "%");
        return statusLines;
    }

    protected Float getAudioRendererCpuLoad() {
        return this.audioSystem.getCpuLoad();
    }

}
