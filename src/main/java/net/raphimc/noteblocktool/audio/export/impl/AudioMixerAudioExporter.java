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
package net.raphimc.noteblocktool.audio.export.impl;

import net.raphimc.audiomixer.AudioMixer;
import net.raphimc.audiomixer.pcmsource.impl.MonoStaticPcmSource;
import net.raphimc.audiomixer.sound.impl.SubMixSound;
import net.raphimc.audiomixer.sound.impl.pcm.OptimizedMonoSound;
import net.raphimc.audiomixer.soundmodifier.impl.NormalizationModifier;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.audiomixer.util.SoundSampleUtil;
import net.raphimc.audiomixer.util.io.SoundIO;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.export.AudioExporter;
import net.raphimc.noteblocktool.util.SoundFileUtil;
import net.raphimc.noteblocktool.util.ThreadedSubMixSound;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AudioMixerAudioExporter extends AudioExporter {

    private final Map<String, float[]> sounds;
    private final AudioMixer audioMixer;
    private final SubMixSound masterMixSound;

    public AudioMixerAudioExporter(final Song song, final PcmFloatAudioFormat audioFormat, final float masterVolume, final boolean globalNormalization, final boolean threaded, final Consumer<Float> progressConsumer) {
        super(song, audioFormat, masterVolume, progressConsumer);

        try {
            this.sounds = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : SoundMap.loadSoundData(song).entrySet()) {
                this.sounds.put(entry.getKey(), SoundIO.readSamples(SoundFileUtil.readAudioFile(new ByteArrayInputStream(entry.getValue())), new PcmFloatAudioFormat(audioFormat.getSampleRate(), 1)));
            }
            this.audioMixer = new AudioMixer(audioFormat);

            if (!threaded) {
                this.masterMixSound = this.audioMixer.getMasterMixSound();
                this.masterMixSound.setMaxSounds(8192);
            } else {
                this.masterMixSound = new ThreadedSubMixSound(Runtime.getRuntime().availableProcessors(), 8192);
                this.audioMixer.playSound(this.masterMixSound);
            }
            if (!globalNormalization) {
                this.masterMixSound.getSoundModifiers().append(new NormalizationModifier());
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize AudioMixer audio exporter", e);
        }
    }

    @Override
    public void render() throws InterruptedException {
        super.render();
        if (this.masterMixSound.getSoundModifiers().getFirst(NormalizationModifier.class) == null) {
            SoundSampleUtil.normalize(this.samples.getArrayDirect());
        }
        if (this.masterMixSound instanceof ThreadedSubMixSound threadedSubMixSound) {
            threadedSubMixSound.close();
        }
    }

    @Override
    protected void processSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.masterMixSound.playSound(new OptimizedMonoSound(new MonoStaticPcmSource(this.sounds.get(sound)), pitch, volume, panning));
    }

    @Override
    protected void mix(final int samplesPerTick) {
        this.samples.add(this.audioMixer.mix(samplesPerTick * this.audioFormat.getChannels()));
    }

}
