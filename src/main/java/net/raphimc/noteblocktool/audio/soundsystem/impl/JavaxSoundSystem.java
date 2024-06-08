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
import net.raphimc.noteblocktool.util.CircularBuffer;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JavaxSoundSystem extends SoundSystem {

    private static final AudioFormat FORMAT = new AudioFormat(44100, 16, 2, true, false);

    protected final Map<String, int[]> sounds;
    protected final List<SoundInstance> playingSounds = new ArrayList<>();
    protected final int samplesPerTick;
    protected final SourceDataLine dataLine;
    protected float masterVolume = 1F;
    protected float[] volumeDividers;
    private final int volumeDividersLength;

    public JavaxSoundSystem(final int maxSounds, final float playbackSpeed) {
        super(maxSounds);

        try {
            this.sounds = SoundMap.loadInstrumentSamples(FORMAT);
            this.samplesPerTick = (int) (FORMAT.getSampleRate() / playbackSpeed) * FORMAT.getChannels();
            this.dataLine = AudioSystem.getSourceDataLine(FORMAT);
            this.dataLine.open(FORMAT, (int) FORMAT.getSampleRate());
            this.dataLine.start();
            this.volumeDividersLength = (int) Math.ceil(playbackSpeed * 3F); // 3s window
        } catch (Throwable e) {
            throw new RuntimeException("Could not initialize javax sound system", e);
        }
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.sounds.containsKey(sound)) return;

        if (this.playingSounds.size() >= this.maxSounds) {
            this.playingSounds.remove(0);
        }

        this.playingSounds.add(new SoundInstance(this.sounds.get(sound), pitch, volume * this.masterVolume, panning));
    }

    @Override
    public synchronized void writeSamples() {
        final long[] samples = new long[this.samplesPerTick];
        for (SoundInstance playingSound : this.playingSounds) {
            playingSound.render();
            playingSound.write(samples);
        }
        this.dataLine.write(this.writeNormalized(samples), 0, samples.length * 2);
        this.playingSounds.removeIf(SoundInstance::isFinished);
    }

    @Override
    public synchronized void stopSounds() {
        this.dataLine.flush();
        this.playingSounds.clear();
        this.volumeDividers = null;
    }

    @Override
    public synchronized void close() {
        this.dataLine.stop();
    }

    @Override
    public synchronized String getStatusLine() {
        return "Sounds: " + this.playingSounds.size() + " / " + this.maxSounds;
    }

    @Override
    public synchronized void setMasterVolume(final float volume) {
        this.masterVolume = volume;
    }

    protected byte[] writeNormalized(final long[] samples) {
        final byte[] out = new byte[samples.length * 2];
        final long max = SoundSampleUtil.getMax(samples);
        float div = Math.max(1, (float) max / Short.MAX_VALUE);
        if (this.volumeDividers == null) {
            this.volumeDividers = new float[this.volumeDividersLength];
            Arrays.fill(this.volumeDividers, div);
        }
        this.volumeDividers[this.volumeDividers.length - 1] = div;
        for (int i = 0; i < this.volumeDividers.length; i++) {
            final float weight = (float) i / this.volumeDividers.length;
            div = Math.max(div, this.volumeDividers[i] * weight);
        }
        System.arraycopy(this.volumeDividers, 1, this.volumeDividers, 0, this.volumeDividers.length - 1);
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
        private final int sliceLength;
        private final int[] mutationBuffer;
        private final int[] outputBuffer;
        private final CircularBuffer mutatedSamplesBuffer;
        private int cursor = 0;

        public SoundInstance(final int[] samples, final float pitch, final float volume, final float panning) {
            this.samples = samples;
            this.pitch = pitch;
            this.volume = volume;
            this.panning = panning;
            this.sliceLength = (int) (JavaxSoundSystem.this.samplesPerTick / FORMAT.getChannels() * pitch) * FORMAT.getChannels() + FORMAT.getChannels();
            this.mutationBuffer = new int[JavaxSoundSystem.this.samplesPerTick * 2];
            this.outputBuffer = new int[JavaxSoundSystem.this.samplesPerTick];
            this.mutatedSamplesBuffer = new CircularBuffer(JavaxSoundSystem.this.samplesPerTick * 3);
        }

        public void render() {
            if (!this.hasDataToRender()) return;

            final int sliceLength = Math.min(this.samples.length - this.cursor, this.sliceLength);
            final long result = SoundSampleUtil.mutate(FORMAT, this.samples, this.cursor, sliceLength, this.pitch, this.volume, this.panning, this.mutationBuffer);
            final int mutationBufferAvailable = (int) (result >> 32);
            if (this.mutatedSamplesBuffer.hasSpaceFor(mutationBufferAvailable)) {
                this.mutatedSamplesBuffer.addAll(this.mutationBuffer, mutationBufferAvailable);
                if ((int) result > 0) {
                    this.cursor += (int) result;
                } else {
                    this.cursor += sliceLength;
                }
            }
        }

        public void write(final long[] buffer) {
            if (buffer.length < this.outputBuffer.length) {
                throw new IllegalArgumentException("Buffer is too small");
            }
            this.mutatedSamplesBuffer.takeAllSafe(this.outputBuffer);
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] += this.outputBuffer[i];
            }
        }

        public boolean hasDataToRender() {
            return this.cursor < this.samples.length;
        }

        public boolean isFinished() {
            return !this.hasDataToRender() && this.mutatedSamplesBuffer.isEmpty();
        }

    }

}
