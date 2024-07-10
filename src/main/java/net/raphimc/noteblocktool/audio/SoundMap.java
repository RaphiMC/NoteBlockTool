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
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SoundMap {

    public static final Map<Instrument, String> INSTRUMENT_SOUNDS = new EnumMap<>(Instrument.class);
    public static final Map<String, URL> SOUND_LOCATIONS = new HashMap<>();

    static {
        INSTRUMENT_SOUNDS.put(Instrument.HARP, "harp.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.BASS, "bass.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.BASS_DRUM, "bd.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.SNARE, "snare.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.HAT, "hat.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.GUITAR, "guitar.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.FLUTE, "flute.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.BELL, "bell.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.CHIME, "icechime.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.XYLOPHONE, "xylobone.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.IRON_XYLOPHONE, "iron_xylophone.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.COW_BELL, "cow_bell.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.DIDGERIDOO, "didgeridoo.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.BIT, "bit.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.BANJO, "banjo.ogg");
        INSTRUMENT_SOUNDS.put(Instrument.PLING, "pling.ogg");

        reload(null);
    }

    public static void reload(final File customSoundsFolder) {
        SOUND_LOCATIONS.clear();
        for (Map.Entry<Instrument, String> entry : INSTRUMENT_SOUNDS.entrySet()) {
            SOUND_LOCATIONS.put(entry.getValue(), SoundMap.class.getResource("/noteblock_sounds/" + entry.getValue()));
        }

        if (customSoundsFolder != null && customSoundsFolder.exists() && customSoundsFolder.isDirectory()) {
            try {
                Files.walk(customSoundsFolder.toPath()).forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) return;

                        final String fileName = customSoundsFolder.toPath().relativize(path).toString();
                        if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                            SOUND_LOCATIONS.put(fileName.replace(File.separatorChar, '/'), path.toUri().toURL());
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException("Error while loading custom sound sample", e);
                    }
                });
            } catch (Throwable e) {
                throw new RuntimeException("Failed to load custom sound samples", e);
            }
        }
    }

    public static Map<String, int[]> loadInstrumentSamples(final AudioFormat targetFormat) {
        try {
            final Map<String, int[]> soundSamples = new HashMap<>();
            for (Map.Entry<String, URL> entry : SOUND_LOCATIONS.entrySet()) {
                soundSamples.put(entry.getKey(), SoundSampleUtil.readSamples(entry.getValue().openStream(), targetFormat));
            }
            return soundSamples;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
    }

}
