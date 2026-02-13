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
package net.raphimc.noteblocktool.audio.system.impl;

import net.raphimc.audiomixer.NormalizedAudioMixer;
import net.raphimc.audiomixer.SourceDataLineAudioMixer;
import net.raphimc.audiomixer.io.AudioIO;
import net.raphimc.audiomixer.pcmsource.impl.MonoStaticPcmSource;
import net.raphimc.audiomixer.sound.impl.mix.MixSound;
import net.raphimc.audiomixer.sound.impl.mix.ThreadedChannelMixSound;
import net.raphimc.audiomixer.sound.impl.pcm.OptimizedMonoSound;
import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocktool.audio.system.AudioSystem;
import net.raphimc.noteblocktool.util.SoundFileUtil;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioMixerAudioSystem extends AudioSystem {

    private static final AudioFormat PLAYBACK_AUDIO_FORMAT = new AudioFormat(48000, Short.SIZE, 2, true, false);

    private final Map<String, float[]> sounds = new HashMap<>();
    private final NormalizedAudioMixer audioMixer;
    private final MixSound masterMixSound;

    public AudioMixerAudioSystem(final Map<String, byte[]> soundData, final int maxSounds, final boolean threaded) {
        super(soundData, maxSounds);
        try {
            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.sounds.put(entry.getKey(), AudioIO.readSamples(SoundFileUtil.readAudioFile(new ByteArrayInputStream(entry.getValue())), new PcmFloatAudioFormat(PLAYBACK_AUDIO_FORMAT.getSampleRate(), 1)));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
        try {
            this.audioMixer = new SourceDataLineAudioMixer(javax.sound.sampled.AudioSystem.getSourceDataLine(PLAYBACK_AUDIO_FORMAT));
            if (threaded) {
                this.masterMixSound = new ThreadedChannelMixSound(Math.max(1, Runtime.getRuntime().availableProcessors() - 2));
                this.audioMixer.playSound(this.masterMixSound);
            } else {
                this.masterMixSound = this.audioMixer.getMasterMixSound();
            }
            this.masterMixSound.setMaxSounds(maxSounds);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize AudioMixer", e);
        }
    }

    public AudioMixerAudioSystem(final Map<String, byte[]> soundData, final int maxSounds, final boolean normalized, final boolean threaded, final PcmFloatAudioFormat loopbackAudioFormat) {
        super(soundData, maxSounds, loopbackAudioFormat);
        try {
            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.sounds.put(entry.getKey(), AudioIO.readSamples(SoundFileUtil.readAudioFile(new ByteArrayInputStream(entry.getValue())), new PcmFloatAudioFormat(loopbackAudioFormat.getSampleRate(), 1)));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }
        this.audioMixer = new NormalizedAudioMixer(loopbackAudioFormat);
        if (!normalized) {
            this.audioMixer.getSoundModifiers().remove(this.audioMixer.getNormalizationModifier());
        }
        if (threaded) {
            this.masterMixSound = new ThreadedChannelMixSound(Math.max(1, Runtime.getRuntime().availableProcessors() - 2));
            this.audioMixer.playSound(this.masterMixSound);
        } else {
            this.masterMixSound = this.audioMixer.getMasterMixSound();
        }
        this.masterMixSound.setMaxSounds(maxSounds);
    }

    @Override
    public void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.masterMixSound.playSound(new OptimizedMonoSound(new MonoStaticPcmSource(this.sounds.get(sound)), pitch, volume, panning));
    }

    @Override
    public void stopAllSounds() {
        this.masterMixSound.stopAllSounds();
        this.audioMixer.getNormalizationModifier().reset();
    }

    @Override
    public float[] render(final int frameCount) {
        return this.audioMixer.render(frameCount);
    }

    @Override
    public void close() {
        if (this.masterMixSound instanceof ThreadedChannelMixSound threadedMixSound) {
            threadedMixSound.close();
        }
        if (this.audioMixer instanceof SourceDataLineAudioMixer sourceDataLineAudioMixer) {
            sourceDataLineAudioMixer.close();
        }
    }

    @Override
    public void setMasterVolume(final float volume) {
        this.audioMixer.setMasterVolume(volume);
    }

    @Override
    public int getPlayingSounds() {
        return this.masterMixSound.getMixedSounds();
    }

    @Override
    public Float getCpuLoad() {
        if (this.audioMixer instanceof SourceDataLineAudioMixer sourceDataLineAudioMixer) {
            return sourceDataLineAudioMixer.getSourceDataLineWriter().getCpuLoad();
        } else {
            return this.masterMixSound.getCpuLoad();
        }
    }

}
