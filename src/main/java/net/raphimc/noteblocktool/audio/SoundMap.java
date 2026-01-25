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
package net.raphimc.noteblocktool.audio;

import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.util.IOUtil;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SoundMap {

    public static final Map<MinecraftInstrument, String> INSTRUMENT_SOUNDS = new EnumMap<>(MinecraftInstrument.class);
    private static final Map<String, URL> ALL_SOUND_LOCATIONS = new HashMap<>();

    static {
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.HARP, "harp.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.BASS, "bass.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.BASS_DRUM, "bd.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.SNARE, "snare.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.HAT, "hat.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.GUITAR, "guitar.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.FLUTE, "flute.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.BELL, "bell.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.CHIME, "icechime.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.XYLOPHONE, "xylobone.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.IRON_XYLOPHONE, "iron_xylophone.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.COW_BELL, "cow_bell.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.DIDGERIDOO, "didgeridoo.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.BIT, "bit.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.BANJO, "banjo.ogg");
        INSTRUMENT_SOUNDS.put(MinecraftInstrument.PLING, "pling.ogg");

        try {
            SoundMap.reload(new File(System.getProperty("user.home"), "Minecraft Note Block Studio/Data/Sounds"));
        } catch (Throwable t) {
            System.err.println("Failed to load custom sounds from Minecraft NoteBlock Studio folder");
            t.printStackTrace();
            reload(null);
        }
    }

    public static void reload(final File customSoundsFolder) {
        ALL_SOUND_LOCATIONS.clear();
        for (Map.Entry<MinecraftInstrument, String> entry : INSTRUMENT_SOUNDS.entrySet()) {
            ALL_SOUND_LOCATIONS.put(entry.getValue(), SoundMap.class.getResource("/noteblock_sounds/" + entry.getValue()));
        }

        if (customSoundsFolder != null && customSoundsFolder.exists() && customSoundsFolder.isDirectory()) {
            try {
                Files.walk(customSoundsFolder.toPath()).forEach(path -> {
                    try {
                        if (Files.isDirectory(path)) return;

                        final String fileName = customSoundsFolder.toPath().relativize(path).toString();
                        if (fileName.endsWith(".ogg") || fileName.endsWith(".wav")) {
                            ALL_SOUND_LOCATIONS.put(fileName.replace(File.separatorChar, '/'), path.toUri().toURL());
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

    public static Map<String, byte[]> loadSoundData(final Song song) {
        try {
            final Map<String, byte[]> soundData = new HashMap<>();
            for (MinecraftInstrument instrument : SongUtil.getUsedVanillaInstruments(song)) {
                final String sound = INSTRUMENT_SOUNDS.get(instrument);
                if (sound != null && ALL_SOUND_LOCATIONS.containsKey(sound)) {
                    soundData.put(sound, IOUtil.readFully(ALL_SOUND_LOCATIONS.get(sound).openStream()));
                }
            }
            for (NbsCustomInstrument customInstrument : SongUtil.getUsedNbsCustomInstruments(song)) {
                final String fileName = customInstrument.getSoundFilePathOr("").replace(File.separatorChar, '/');
                if (ALL_SOUND_LOCATIONS.containsKey(fileName)) {
                    soundData.put(fileName, IOUtil.readFully(ALL_SOUND_LOCATIONS.get(fileName).openStream()));
                }
            }
            return soundData;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
    }

}
