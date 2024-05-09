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
import net.raphimc.noteblocktool.audio.soundsystem.impl.JavaxSoundSystem;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import java.util.EnumMap;
import java.util.Map;

public class SoundMap {

    public static final Map<Instrument, String> SOUNDS = new EnumMap<>(Instrument.class);

    static {
        SOUNDS.put(Instrument.HARP, "/noteblock_sounds/harp.ogg");
        SOUNDS.put(Instrument.BASS, "/noteblock_sounds/bass.ogg");
        SOUNDS.put(Instrument.BASS_DRUM, "/noteblock_sounds/bd.ogg");
        SOUNDS.put(Instrument.SNARE, "/noteblock_sounds/snare.ogg");
        SOUNDS.put(Instrument.HAT, "/noteblock_sounds/hat.ogg");
        SOUNDS.put(Instrument.GUITAR, "/noteblock_sounds/guitar.ogg");
        SOUNDS.put(Instrument.FLUTE, "/noteblock_sounds/flute.ogg");
        SOUNDS.put(Instrument.BELL, "/noteblock_sounds/bell.ogg");
        SOUNDS.put(Instrument.CHIME, "/noteblock_sounds/icechime.ogg");
        SOUNDS.put(Instrument.XYLOPHONE, "/noteblock_sounds/xylobone.ogg");
        SOUNDS.put(Instrument.IRON_XYLOPHONE, "/noteblock_sounds/iron_xylophone.ogg");
        SOUNDS.put(Instrument.COW_BELL, "/noteblock_sounds/cow_bell.ogg");
        SOUNDS.put(Instrument.DIDGERIDOO, "/noteblock_sounds/didgeridoo.ogg");
        SOUNDS.put(Instrument.BIT, "/noteblock_sounds/bit.ogg");
        SOUNDS.put(Instrument.BANJO, "/noteblock_sounds/banjo.ogg");
        SOUNDS.put(Instrument.PLING, "/noteblock_sounds/pling.ogg");
    }

    public static Map<Instrument, int[]> loadInstrumentSamples(final AudioFormat targetFormat) {
        try {
            final Map<Instrument, int[]> instrumentSamples = new EnumMap<>(Instrument.class);
            for (Map.Entry<Instrument, String> entry : SOUNDS.entrySet()) {
                instrumentSamples.put(entry.getKey(), SoundSampleUtil.readSamples(JavaxSoundSystem.class.getResourceAsStream(entry.getValue()), targetFormat));
            }
            return instrumentSamples;
        } catch (Throwable e) {
            throw new RuntimeException("Could not load instrument samples", e);
        }
    }

}
