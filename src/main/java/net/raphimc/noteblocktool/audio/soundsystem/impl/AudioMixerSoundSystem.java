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
package net.raphimc.noteblocktool.audio.soundsystem.impl;

import net.raphimc.audiomixer.SourceDataLineAudioMixer;
import net.raphimc.audiomixer.pcmsource.impl.MonoIntPcmSource;
import net.raphimc.audiomixer.sound.impl.pcm.OptimizedMonoSound;
import net.raphimc.audiomixer.util.AudioFormats;
import net.raphimc.audiomixer.util.io.SoundIO;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.SoundFileUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class AudioMixerSoundSystem extends SoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(48000, 16, 2, true, false);

    protected final Map<String, int[]> sounds;
    protected final SourceDataLineAudioMixer audioMixer;

    public AudioMixerSoundSystem(final Map<String, byte[]> soundData, final int maxSounds, final float playbackSpeed) {
        super(maxSounds);

        try {
            this.sounds = new HashMap<>();
            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.sounds.put(entry.getKey(), SoundIO.readSamples(SoundFileUtil.readAudioFile(new ByteArrayInputStream(entry.getValue())), AudioFormats.withChannels(FORMAT, 1)));
            }
            this.audioMixer = new SourceDataLineAudioMixer(AudioSystem.getSourceDataLine(FORMAT), (int) Math.ceil(FORMAT.getSampleRate() / playbackSpeed) * FORMAT.getChannels());
            this.audioMixer.getMasterMixSound().setMaxSounds(maxSounds);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to initialize AudioMixer sound system", e);
        }
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        this.audioMixer.playSound(new OptimizedMonoSound(new MonoIntPcmSource(this.sounds.get(sound)), pitch, volume, panning));
    }

    @Override
    public synchronized void postTick() {
        this.audioMixer.mixSlice();
    }

    @Override
    public synchronized void stopSounds() {
        final SourceDataLine sourceDataLine = this.audioMixer.getSourceDataLine();
        sourceDataLine.stop();
        sourceDataLine.flush();
        sourceDataLine.start();
        this.audioMixer.stopAllSounds();
    }

    @Override
    public synchronized void close() {
        this.audioMixer.close();
    }

    @Override
    public synchronized String getStatusLine() {
        return "Sounds: " + this.audioMixer.getMasterMixSound().getMixedSounds() + " / " + this.maxSounds;
    }

    @Override
    public synchronized void setMasterVolume(final float volume) {
        this.audioMixer.setMasterVolume(volume);
    }

}
