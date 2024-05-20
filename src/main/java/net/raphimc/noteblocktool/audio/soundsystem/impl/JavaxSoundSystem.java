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

import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class JavaxSoundSystem extends SoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 2, true, false);

    protected final Map<String, int[]> sounds;
    protected final List<SoundInstance> playingSounds = new CopyOnWriteArrayList<>();
    protected final int samplesPerTick;
    protected final long availableNanosPerTick;
    protected final SourceDataLine dataLine;
    protected float masterVolume = 1F;
    protected long neededNanosPerTick = 0L;

    public JavaxSoundSystem(final int maxSounds, final float playbackSpeed) {
        super(maxSounds);

        try {
            this.sounds = SoundMap.loadInstrumentSamples(FORMAT);
            this.samplesPerTick = (int) (FORMAT.getSampleRate() / playbackSpeed) * FORMAT.getChannels();
            this.availableNanosPerTick = (long) (1_000_000_000L / playbackSpeed);
            this.dataLine = AudioSystem.getSourceDataLine(FORMAT);
            this.dataLine.open(FORMAT, (int) FORMAT.getSampleRate());
            this.dataLine.start();
        } catch (Throwable e) {
            throw new RuntimeException("Could not initialize javax sound system", e);
        }
    }

    @Override
    public void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        if (this.playingSounds.size() >= this.maxSounds) {
            this.playingSounds.remove(0);
        }

        this.playingSounds.add(new SoundInstance(this.sounds.get(sound), pitch, volume * this.masterVolume, panning));
    }

    @Override
    public void writeSamples() {
        final long start = System.nanoTime();
        final long[] samples = new long[this.samplesPerTick];
        for (SoundInstance playingSound : this.playingSounds) {
            playingSound.write(samples);
        }
        this.dataLine.write(this.write(samples), 0, samples.length * 2);

        this.playingSounds.removeIf(SoundInstance::isFinished);
        this.neededNanosPerTick = System.nanoTime() - start;
    }

    @Override
    public void stopSounds() {
        this.dataLine.flush();
        this.playingSounds.clear();
    }

    @Override
    public void close() {
        this.dataLine.stop();
    }

    @Override
    public String getStatusLine() {
        final float load = (float) this.neededNanosPerTick / this.availableNanosPerTick;
        return "Sounds: " + this.playingSounds.size() + " / " + this.maxSounds + ", CPU Load: " + (int) (load * 100) + "%";
    }

    @Override
    public void setMasterVolume(final float volume) {
        this.masterVolume = volume;
    }

    protected byte[] write(final long[] samples) {
        final byte[] out = new byte[samples.length * 2];
        final long max = SoundSampleUtil.getMax(samples);
        final float div = Math.max(1, (float) max / Short.MAX_VALUE);
        for (int i = 0; i < samples.length; i++) {
            final short conv = (short) (samples[i] / div);
            out[i * 2] = (byte) (conv & 0xFF);
            out[i * 2 + 1] = (byte) ((conv >> 8) & 0xFF);
        }
        return out;
    }

    protected class SoundInstance {

        private final int[] samples;
        private final float pitch;
        private final float volume;
        private final float panning;
        private final int step;
        private final int[] sliceBuffer;
        private int cursor = 0;

        public SoundInstance(final int[] samples, final float pitch, final float volume, final float panning) {
            this.samples = samples;
            this.pitch = pitch;
            this.volume = volume;
            this.panning = panning;
            this.step = (int) (JavaxSoundSystem.this.samplesPerTick / FORMAT.getChannels() * pitch) * FORMAT.getChannels();
            this.sliceBuffer = new int[this.step + FORMAT.getChannels()];
        }

        public int[] render() {
            final int copyLength = Math.min(this.samples.length - this.cursor, this.sliceBuffer.length);
            System.arraycopy(this.samples, this.cursor, this.sliceBuffer, 0, copyLength);
            Arrays.fill(this.sliceBuffer, copyLength, this.sliceBuffer.length, 0);
            this.cursor += this.step;

            return SoundSampleUtil.mutate(FORMAT, this.sliceBuffer, this.pitch, this.volume, this.panning);
        }

        public void write(final long[] buffer) {
            final int[] mutatedSlice = this.render();
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] += mutatedSlice[i];
            }
        }

        public boolean isFinished() {
            return this.cursor >= this.samples.length;
        }

    }

}
