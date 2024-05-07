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
package net.raphimc.noteblocktool.audio;

import net.raphimc.noteblocklib.util.Instrument;

import java.util.HashMap;
import java.util.Map;

public class SoundMap {

    public static final Map<Instrument, String> SOUNDS = new HashMap<>();

    static {
        SOUNDS.put(Instrument.HARP, "/noteblock_sounds/harp.wav");
        SOUNDS.put(Instrument.BASS, "/noteblock_sounds/bass.wav");
        SOUNDS.put(Instrument.BASS_DRUM, "/noteblock_sounds/bd.wav");
        SOUNDS.put(Instrument.SNARE, "/noteblock_sounds/snare.wav");
        SOUNDS.put(Instrument.HAT, "/noteblock_sounds/hat.wav");
        SOUNDS.put(Instrument.GUITAR, "/noteblock_sounds/guitar.wav");
        SOUNDS.put(Instrument.FLUTE, "/noteblock_sounds/flute.wav");
        SOUNDS.put(Instrument.BELL, "/noteblock_sounds/bell.wav");
        SOUNDS.put(Instrument.CHIME, "/noteblock_sounds/icechime.wav");
        SOUNDS.put(Instrument.XYLOPHONE, "/noteblock_sounds/xylobone.wav");
        SOUNDS.put(Instrument.IRON_XYLOPHONE, "/noteblock_sounds/iron_xylophone.wav");
        SOUNDS.put(Instrument.COW_BELL, "/noteblock_sounds/cow_bell.wav");
        SOUNDS.put(Instrument.DIDGERIDOO, "/noteblock_sounds/didgeridoo.wav");
        SOUNDS.put(Instrument.BIT, "/noteblock_sounds/bit.wav");
        SOUNDS.put(Instrument.BANJO, "/noteblock_sounds/banjo.wav");
        SOUNDS.put(Instrument.PLING, "/noteblock_sounds/pling.wav");
    }

}
