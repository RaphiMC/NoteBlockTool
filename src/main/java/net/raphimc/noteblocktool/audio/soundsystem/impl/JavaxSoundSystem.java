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
package net.raphimc.noteblocktool.audio.soundsystem.impl;

import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JavaxSoundSystem extends SoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 1, true, false);

    private final Map<Instrument, int[]> sounds;
    private final int samplesPerTick;
    private final SourceDataLine dataLine;
    private final Map<String, int[]> mutationCache;
    private long[] buffer = new long[0];

    public JavaxSoundSystem(final float playbackSpeed) {
        super(0);

        try {
            this.sounds = SoundMap.loadInstrumentSamples(FORMAT);
            this.samplesPerTick = (int) (FORMAT.getSampleRate() / playbackSpeed);
            this.dataLine = AudioSystem.getSourceDataLine(FORMAT);
            this.dataLine.open(FORMAT, (int) FORMAT.getSampleRate());
            this.dataLine.start();
            this.mutationCache = new HashMap<>();
        } catch (Throwable e) {
            throw new RuntimeException("Could not initialize javax audio system", e);
        }
    }

    @Override
    public void playNote(final Instrument instrument, final float volume, final float pitch, final float panning) {
        final String key = instrument.ordinal() + "\0" + volume + "\0" + pitch;
        final int[] samples = this.mutationCache.computeIfAbsent(key, k -> SoundSampleUtil.mutate(this.sounds.get(instrument), volume * this.masterVolume, pitch));
        if (this.buffer.length < samples.length) this.buffer = Arrays.copyOf(this.buffer, samples.length);
        for (int i = 0; i < samples.length; i++) this.buffer[i] += samples[i];
    }

    @Override
    public void writeSamples() {
        final long[] samples = Arrays.copyOfRange(this.buffer, 0, this.samplesPerTick);
        this.dataLine.write(this.write(samples), 0, samples.length * 2);
        if (this.buffer.length > this.samplesPerTick) this.buffer = Arrays.copyOfRange(this.buffer, this.samplesPerTick, this.buffer.length);
        else if (this.buffer.length != 0) this.buffer = new long[0];
    }

    @Override
    public void stopSounds() {
        this.dataLine.flush();
    }

    @Override
    public void close() {
        this.dataLine.stop();
    }

    @Override
    public int getSoundCount() {
        return 0;
    }

    @Override
    public void setMasterVolume(final float volume) {
        super.setMasterVolume(volume);
        this.mutationCache.clear();
    }

    private byte[] write(final long[] samples) {
        final byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            long sample = samples[i];
            if (sample > Short.MAX_VALUE) sample = Short.MAX_VALUE;
            else if (sample < Short.MIN_VALUE) sample = Short.MIN_VALUE;

            final short conv = (short) sample;
            out[i * 2] = (byte) (conv & 0xFF);
            out[i * 2 + 1] = (byte) ((conv >> 8) & 0xFF);
        }
        return out;
    }

}
