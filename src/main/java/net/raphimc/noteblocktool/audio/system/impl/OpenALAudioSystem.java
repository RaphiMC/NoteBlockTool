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

import net.raphimc.audiomixer.util.PcmFloatAudioFormat;
import net.raphimc.noteblocktool.audio.system.AudioSystem;
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SoundFileUtil;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenALAudioSystem extends AudioSystem {

    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final List<Integer> playingSources = new ArrayList<>();
    private long device;
    private long context;

    public OpenALAudioSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        super(soundData, maxSounds);

        this.device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (this.device == 0L) {
            throw new RuntimeException("Failed to open device");
        }
        this.createAlcCapabilities();
        this.context = ALC10.alcCreateContext(this.device, new int[]{
                ALC11.ALC_MONO_SOURCES, maxSounds,
                SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT, ALC10.ALC_TRUE,
                0
        });
        if (this.context == 0L) {
            this.checkAlcError("Failed to create context");
            throw new RuntimeException("Failed to create context");
        }

        this.bindContext();
        try {
            this.createAlCapabilities();
            this.configureFor2D();

            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.soundBuffers.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
            }

            System.out.println("Initialized OpenAL " + AL10.alGetString(AL10.AL_VERSION) + " on " + ALC10.alcGetString(this.device, ALC11.ALC_ALL_DEVICES_SPECIFIER));
        } finally {
            this.unbindContext();
        }
    }

    public OpenALAudioSystem(final Map<String, byte[]> soundData, final int maxSounds, final PcmFloatAudioFormat loopbackAudioFormat) {
        super(soundData, maxSounds, loopbackAudioFormat);
        if (ALC10.alcGetCurrentContext() != 0L) {
            throw new IllegalStateException("OpenAL audio system can only be initialized once per thread");
        }

        this.device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
        if (this.device == 0L) {
            throw new RuntimeException("Failed to open device");
        }
        final ALCCapabilities alcCapabilities = this.createAlcCapabilities();
        if (!alcCapabilities.ALC_SOFT_loopback) {
            throw new RuntimeException("ALC_SOFT_loopback is not supported");
        }
        this.context = ALC10.alcCreateContext(this.device, new int[]{
                ALC11.ALC_MONO_SOURCES, maxSounds,
                SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT, ALC10.ALC_TRUE,
                ALC10.ALC_FREQUENCY, (int) loopbackAudioFormat.getSampleRate(),
                SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, this.getAlSoftChannelFormat(loopbackAudioFormat),
                SOFTLoopback.ALC_FORMAT_TYPE_SOFT, SOFTLoopback.ALC_FLOAT_SOFT,
                0
        });
        if (this.context == 0L) {
            this.checkAlcError("Failed to create context");
            throw new RuntimeException("Failed to create context");
        }

        this.bindContext();
        try {
            this.createAlCapabilities();
            this.configureFor2D();

            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.soundBuffers.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
            }

            System.out.println("Initialized OpenAL " + AL10.alGetString(AL10.AL_VERSION) + " on " + ALC10.alcGetString(this.device, ALC11.ALC_ALL_DEVICES_SPECIFIER));
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public synchronized void preTick() {
        this.bindContext();
        try {
            this.playingSources.removeIf(source -> {
                final int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
                this.checkAlError("Failed to get audio source state");
                if (state == AL10.AL_STOPPED) {
                    AL10.alDeleteSources(source);
                    this.checkAlError("Failed to delete audio source");
                    return true;
                }
                return false;
            });
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        this.bindContext();
        try {
            if (!this.soundBuffers.containsKey(sound)) return;

            if (this.playingSources.size() >= this.getMaxSounds()) {
                AL10.alDeleteSources(this.playingSources.remove(0));
                this.checkAlError("Failed to delete audio source");
            }

            final int source = AL10.alGenSources();
            this.checkAlError("Failed to generate audio source");
            AL10.alSourcei(source, AL10.AL_BUFFER, this.soundBuffers.get(sound));
            this.checkAlError("Failed to set audio source buffer");
            AL10.alSourcef(source, AL10.AL_PITCH, pitch);
            this.checkAlError("Failed to set audio source pitch");
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            this.checkAlError("Failed to set audio source volume");
            AL10.alSource3f(source, AL10.AL_POSITION, panning * 2F, 0F, 0F);
            this.checkAlError("Failed to set audio source position");
            AL10.alSourcePlay(source);
            this.checkAlError("Failed to play audio source");
            this.playingSources.add(source);
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public synchronized void stopAllSounds() {
        this.bindContext();
        try {
            for (int source : this.playingSources) {
                AL10.alDeleteSources(source);
                this.checkAlError("Failed to delete audio source", AL10.AL_INVALID_NAME);
            }
            this.playingSources.clear();
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public float[] render(final int frameCount) {
        this.bindContext();
        try {
            final int samplesLength = frameCount * this.getLoopbackAudioFormat().getChannels();
            final float[] samples = new float[samplesLength];
            SOFTLoopback.alcRenderSamplesSOFT(this.device, samples, frameCount);
            this.checkAlError("Failed to render samples");
            return samples;
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public synchronized void close() {
        this.soundBuffers.clear();
        this.playingSources.clear();
        AL.setCurrentThread(null);
        if (this.context != 0L) {
            ALC10.alcDestroyContext(this.context);
            this.checkAlcError("Failed to destroy context");
            this.context = 0L;
        }
        ALC.setCapabilities(null);
        if (this.device != 0L) {
            if (!ALC10.alcCloseDevice(this.device)) {
                throw new RuntimeException("Failed to close device");
            }
            this.device = 0L;
        }
    }

    @Override
    public void setMasterVolume(final float volume) {
        this.bindContext();
        try {
            AL10.alListenerf(AL10.AL_GAIN, volume);
            this.checkAlError("Failed to set listener gain");
        } finally {
            this.unbindContext();
        }
    }

    @Override
    public synchronized Integer getPlayingSounds() {
        return this.playingSources.size();
    }

    @Override
    public Float getCpuLoad() {
        return null;
    }

    private ALCCapabilities createAlcCapabilities() {
        final ALCCapabilities alcCapabilities = ALC.createCapabilities(this.device);
        this.checkAlcError("Failed to create ALC capabilities");
        if (!alcCapabilities.OpenALC11) {
            throw new RuntimeException("OpenALC 1.1 is not supported");
        }
        if (!alcCapabilities.ALC_EXT_thread_local_context) {
            throw new RuntimeException("ALC_EXT_thread_local_context is not supported");
        }
        if (!alcCapabilities.ALC_SOFT_output_limiter) {
            throw new RuntimeException("ALC_SOFT_output_limiter is not supported");
        }
        return alcCapabilities;
    }

    private ALCapabilities createAlCapabilities() {
        final ALCapabilities alCapabilities = AL.createCapabilities(ALC.getCapabilities());
        this.checkAlError("Failed to create AL capabilities");
        if (!alCapabilities.OpenAL11) {
            throw new RuntimeException("OpenAL 1.1 is not supported");
        }
        return alCapabilities;
    }

    private void configureFor2D() {
        AL10.alDistanceModel(AL10.AL_NONE);
        this.checkAlError("Failed to set distance model");
        AL10.alListener3f(AL10.AL_POSITION, 0F, 0F, 1F);
        this.checkAlError("Failed to set listener position");
        AL10.alListenerfv(AL10.AL_ORIENTATION, new float[]{0F, 0F, -1F, 0F, 1F, 0F});
        this.checkAlError("Failed to set listener orientation");
    }

    private int loadAudioFile(final byte[] data) {
        try {
            final AudioInputStream audioInputStream = SoundFileUtil.readAudioFile(new ByteArrayInputStream(data));
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final byte[] audioBytes = IOUtil.readFully(audioInputStream);

            final int buffer = AL10.alGenBuffers();
            this.checkAlError("Failed to generate audio buffer");

            final ByteBuffer audioBuffer = MemoryUtil.memAlloc(audioBytes.length).put(audioBytes).flip();
            AL10.alBufferData(buffer, this.getAlAudioFormat(audioFormat), audioBuffer, (int) audioFormat.getSampleRate());
            this.checkAlError("Failed to set audio buffer data");
            MemoryUtil.memFree(audioBuffer);

            return buffer;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load audio file", e);
        }
    }

    private void bindContext() {
        if (!EXTThreadLocalContext.alcSetThreadContext(this.context)) {
            throw new RuntimeException("Failed to make context current");
        }
    }

    private void unbindContext() {
        if (!EXTThreadLocalContext.alcSetThreadContext(0L)) {
            throw new RuntimeException("Failed to unset current context");
        }
    }

    private void checkAlcError(final String message, final int... allowedErrors) {
        final int error = ALC10.alcGetError(this.device);
        if (error != ALC10.ALC_NO_ERROR) {
            for (int ignoreError : allowedErrors) {
                if (error == ignoreError) {
                    return;
                }
            }
            throw new RuntimeException("ALC error: " + message + " (" + error + ")");
        }
    }

    private void checkAlError(final String message, final int... allowedErrors) {
        final int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            for (int ignoreError : allowedErrors) {
                if (error == ignoreError) {
                    return;
                }
            }
            throw new RuntimeException("AL error: " + message + " (" + error + ")");
        }
    }

    private int getAlAudioFormat(final AudioFormat audioFormat) {
        if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED || audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            if (audioFormat.getChannels() == 1) {
                if (audioFormat.getSampleSizeInBits() == 8) {
                    return AL10.AL_FORMAT_MONO8;
                } else if (audioFormat.getSampleSizeInBits() == 16) {
                    return AL10.AL_FORMAT_MONO16;
                }
            } else if (audioFormat.getChannels() == 2) {
                if (audioFormat.getSampleSizeInBits() == 8) {
                    return AL10.AL_FORMAT_STEREO8;
                } else if (audioFormat.getSampleSizeInBits() == 16) {
                    return AL10.AL_FORMAT_STEREO16;
                }
            }
        }

        throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
    }

    private int getAlSoftChannelFormat(final AudioFormat audioFormat) {
        if (audioFormat.getChannels() == 1) {
            return SOFTLoopback.ALC_MONO_SOFT;
        } else if (audioFormat.getChannels() == 2) {
            return SOFTLoopback.ALC_STEREO_SOFT;
        }

        throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
    }

}
