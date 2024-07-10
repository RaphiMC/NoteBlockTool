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
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SampleOutputStream;
import net.raphimc.noteblocktool.util.SoundSampleUtil;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenALSoundSystem extends SoundSystem {

    private static OpenALSoundSystem instance;

    public static OpenALSoundSystem createPlayback(final int maxSounds) {
        if (instance != null) {
            throw new IllegalStateException("OpenAL sound system already initialized");
        }
        instance = new OpenALSoundSystem(maxSounds);
        return instance;
    }

    public static OpenALSoundSystem createCapture(final int maxSounds, final AudioFormat captureAudioFormat) {
        if (instance != null) {
            throw new IllegalStateException("OpenAL sound system already initialized");
        }
        instance = new OpenALSoundSystem(maxSounds, captureAudioFormat);
        return instance;
    }


    private final Map<String, Integer> soundBuffers = new HashMap<>();
    private final List<Integer> playingSources = new ArrayList<>();
    private final AudioFormat captureAudioFormat;
    private long device;
    private long context;
    private Thread shutdownHook;
    private ByteBuffer captureBuffer;

    private OpenALSoundSystem(final int maxSounds) {
        this(maxSounds, null);
    }

    private OpenALSoundSystem(final int maxSounds, final AudioFormat captureAudioFormat) {
        super(maxSounds);

        this.captureAudioFormat = captureAudioFormat;
        final int[] attributes;
        if (captureAudioFormat == null) {
            this.device = ALC10.alcOpenDevice((ByteBuffer) null);
            attributes = new int[]{
                    ALC11.ALC_MONO_SOURCES, this.maxSounds,
                    SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT, ALC10.ALC_TRUE,
                    0
            };
        } else {
            this.device = SOFTLoopback.alcLoopbackOpenDeviceSOFT((ByteBuffer) null);
            attributes = new int[]{
                    ALC11.ALC_MONO_SOURCES, this.maxSounds,
                    SOFTOutputLimiter.ALC_OUTPUT_LIMITER_SOFT, ALC10.ALC_TRUE,
                    ALC10.ALC_FREQUENCY, (int) this.captureAudioFormat.getSampleRate(),
                    SOFTLoopback.ALC_FORMAT_CHANNELS_SOFT, this.getAlSoftChannelFormat(this.captureAudioFormat),
                    SOFTLoopback.ALC_FORMAT_TYPE_SOFT, this.getAlSoftFormatType(this.captureAudioFormat),
                    0
            };
        }
        if (this.device <= 0L) {
            throw new RuntimeException("Failed to open device");
        }
        this.checkALCError("Failed to open device");

        final ALCCapabilities alcCapabilities = ALC.createCapabilities(this.device);
        this.checkALCError("Failed to create alcCapabilities");
        if (!alcCapabilities.OpenALC11) {
            throw new RuntimeException("OpenALC 1.1 is not supported");
        }
        if (!alcCapabilities.ALC_SOFT_output_limiter) {
            throw new RuntimeException("ALC_SOFT_output_limiter is not supported");
        }
        if (captureAudioFormat != null && !alcCapabilities.ALC_SOFT_loopback) {
            throw new RuntimeException("ALC_SOFT_loopback is not supported");
        }

        this.context = ALC10.alcCreateContext(this.device, attributes);
        this.checkALCError("Failed to create context");
        if (!ALC10.alcMakeContextCurrent(this.context)) {
            throw new RuntimeException("Failed to make context current");
        }

        final ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
        this.checkALError("Failed to create alCapabilities");
        if (!alCapabilities.OpenAL11) {
            throw new RuntimeException("OpenAL 1.1 is not supported");
        }

        AL10.alDistanceModel(AL10.AL_NONE);
        this.checkALError("Failed to set distance model");
        AL10.alListener3f(AL10.AL_POSITION, 0F, 0F, 0F);
        this.checkALError("Failed to set listener position");
        AL10.alListenerfv(AL10.AL_ORIENTATION, new float[]{0F, 0F, -1F, 0F, 1F, 0F});
        this.checkALError("Failed to set listener orientation");

        try {
            for (Map.Entry<String, URL> entry : SoundMap.SOUND_LOCATIONS.entrySet()) {
                this.soundBuffers.put(entry.getKey(), this.loadAudioFile(entry.getValue().openStream()));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load sound samples", e);
        }

        Runtime.getRuntime().addShutdownHook(this.shutdownHook = new Thread(() -> {
            this.shutdownHook = null;
            this.close();
        }));

        if (captureAudioFormat != null) {
            this.captureBuffer = MemoryUtil.memAlloc((int) this.captureAudioFormat.getSampleRate() * this.captureAudioFormat.getChannels() * this.captureAudioFormat.getSampleSizeInBits() / 8 * 30);
        }

        System.out.println("Initialized OpenAL " + AL10.alGetString(AL10.AL_VERSION) + " on " + ALC10.alcGetString(this.device, ALC11.ALC_ALL_DEVICES_SPECIFIER));
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.soundBuffers.containsKey(sound)) return;

        if (this.playingSources.size() >= this.maxSounds) {
            AL10.alDeleteSources(this.playingSources.remove(0));
            this.checkALError("Failed to delete audio source");
        }

        final int source = AL10.alGenSources();
        this.checkALError("Failed to generate audio source");
        AL10.alSourcei(source, AL10.AL_BUFFER, this.soundBuffers.get(sound));
        this.checkALError("Failed to set audio source buffer");
        AL10.alSourcef(source, AL10.AL_PITCH, pitch);
        this.checkALError("Failed to set audio source pitch");
        AL10.alSourcef(source, AL10.AL_GAIN, volume);
        this.checkALError("Failed to set audio source volume");
        AL10.alSource3f(source, AL10.AL_POSITION, panning * 2F, 0F, 0F);
        this.checkALError("Failed to set audio source position");
        AL10.alSourcePlay(source);
        this.checkALError("Failed to play audio source");
        this.playingSources.add(source);
    }

    @Override
    public synchronized void tick() {
        this.playingSources.removeIf(source -> {
            final int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
            this.checkALError("Failed to get audio source state");
            if (state == AL10.AL_STOPPED) {
                AL10.alDeleteSources(source);
                this.checkALError("Failed to delete audio source");
                return true;
            }
            return false;
        });
    }

    public synchronized void renderSamples(final SampleOutputStream outputStream, final int sampleCount) {
        final int samplesLength = sampleCount * this.captureAudioFormat.getChannels();
        if (samplesLength * this.captureAudioFormat.getSampleSizeInBits() / 8 > this.captureBuffer.capacity()) {
            throw new IllegalArgumentException("Sample count too high");
        }
        SOFTLoopback.alcRenderSamplesSOFT(this.device, this.captureBuffer, sampleCount);
        this.checkALError("Failed to render samples");
        if (this.captureAudioFormat.getSampleSizeInBits() == 8) {
            for (int i = 0; i < samplesLength; i++) {
                outputStream.writeSample(this.captureBuffer.get(i));
            }
        } else if (this.captureAudioFormat.getSampleSizeInBits() == 16) {
            for (int i = 0; i < samplesLength; i++) {
                outputStream.writeSample(this.captureBuffer.getShort(i * 2));
            }
        } else if (this.captureAudioFormat.getSampleSizeInBits() == 32) {
            for (int i = 0; i < samplesLength; i++) {
                outputStream.writeSample(this.captureBuffer.getInt(i * 4));
            }
        }
    }

    @Override
    public synchronized void stopSounds() {
        for (int source : this.playingSources) {
            AL10.alDeleteSources(source);
            this.checkALError("Failed to delete audio source", AL10.AL_INVALID_NAME);
        }
        this.playingSources.clear();
    }

    @Override
    public synchronized void close() {
        if (this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
        this.soundBuffers.clear();
        this.playingSources.clear();
        if (this.context != 0L) {
            ALC10.alcMakeContextCurrent(0);
            ALC10.alcDestroyContext(this.context);
            this.context = 0L;
        }
        if (this.device != 0L) {
            ALC10.alcCloseDevice(this.device);
            this.device = 0L;
        }
        if (this.captureBuffer != null) {
            MemoryUtil.memFree(this.captureBuffer);
            this.captureBuffer = null;
        }
        instance = null;
    }

    @Override
    public synchronized String getStatusLine() {
        return "Sounds: " + this.playingSources.size() + " / " + this.maxSounds;
    }

    @Override
    public synchronized void setMasterVolume(final float volume) {
        AL10.alListenerf(AL10.AL_GAIN, volume);
        this.checkALError("Failed to set listener gain");
    }

    private int loadAudioFile(final InputStream inputStream) {
        try {
            final AudioInputStream audioInputStream = SoundSampleUtil.readAudioFile(inputStream);
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final byte[] audioBytes = IOUtil.readFully(audioInputStream);

            final int buffer = AL10.alGenBuffers();
            this.checkALError("Failed to generate audio buffer");

            final ByteBuffer audioBuffer = MemoryUtil.memAlloc(audioBytes.length).put(audioBytes);
            audioBuffer.flip();
            AL10.alBufferData(buffer, this.getAlAudioFormat(audioFormat), audioBuffer, (int) audioFormat.getSampleRate());
            this.checkALError("Failed to set audio buffer data");
            MemoryUtil.memFree(audioBuffer);

            return buffer;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load audio file", e);
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
        if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED || audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            if (audioFormat.getChannels() == 1) {
                return SOFTLoopback.ALC_MONO_SOFT;
            } else if (audioFormat.getChannels() == 2) {
                return SOFTLoopback.ALC_STEREO_SOFT;
            }
        }

        throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
    }

    private int getAlSoftFormatType(final AudioFormat audioFormat) {
        if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED || audioFormat.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            if (audioFormat.getSampleSizeInBits() == 8) {
                return SOFTLoopback.ALC_BYTE_SOFT;
            } else if (audioFormat.getSampleSizeInBits() == 16) {
                return SOFTLoopback.ALC_SHORT_SOFT;
            } else if (audioFormat.getSampleSizeInBits() == 32) {
                return SOFTLoopback.ALC_INT_SOFT;
            }
        }

        throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
    }

    private void checkALCError(final String message, final int... allowedErrors) {
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

    private void checkALError(final String message, final int... allowedErrors) {
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

}
