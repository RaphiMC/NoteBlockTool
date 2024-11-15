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

import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.raphimc.noteblocktool.audio.soundsystem.SoundSystem;
import net.raphimc.noteblocktool.audio.soundsystem.XAudio2Library;
import net.raphimc.noteblocktool.util.IOUtil;
import net.raphimc.noteblocktool.util.SoundFileUtil;
import net.raphimc.noteblocktool.util.jna.Ole32;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XAudio2SoundSystem extends SoundSystem {

    private final Map<String, SoundBuffer> soundBuffers = new HashMap<>();
    private final List<XAudio2Library.XAudio2SourceVoice> playingVoices = new ArrayList<>();
    private XAudio2Library.XAudio2 xAudio2;
    private XAudio2Library.XAudio2MasteringVoice masteringVoice;
    private int masteringVoiceChannelMask;
    private int masteringVoiceChannels;
    private Thread shutdownHook;

    public XAudio2SoundSystem(final Map<String, byte[]> soundData, final int maxSounds) {
        super(maxSounds);
        if (!XAudio2Library.isLoaded()) {
            throw new IllegalStateException("XAudio2 library is not available");
        }

        this.checkError(Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED), "Could not initialize COM");

        final PointerByReference ppXAudio2 = new PointerByReference();
        this.checkError(XAudio2Library.INSTANCE.XAudio2Create(ppXAudio2, 0, XAudio2Library.XAUDIO2_ANY_PROCESSOR), "Could not create XAudio2 instance");
        this.xAudio2 = new XAudio2Library.XAudio2(ppXAudio2.getValue());

        final PointerByReference ppMasteringVoice = new PointerByReference();
        this.checkError(this.xAudio2.CreateMasteringVoice(ppMasteringVoice, XAudio2Library.XAUDIO2_DEFAULT_CHANNELS, XAudio2Library.XAUDIO2_DEFAULT_SAMPLERATE, 0, null, null, 0), "Could not create mastering voice");
        this.masteringVoice = new XAudio2Library.XAudio2MasteringVoice(ppMasteringVoice.getValue());
        final IntByReference masteringVoiceChannelMask = new IntByReference();
        this.checkError(this.masteringVoice.GetChannelMask(masteringVoiceChannelMask), "Could not get channel mask");
        this.masteringVoiceChannelMask = masteringVoiceChannelMask.getValue();
        final XAudio2Library.XAUDIO2_VOICE_DETAILS.ByReference masteringVoiceDetails = new XAudio2Library.XAUDIO2_VOICE_DETAILS.ByReference();
        masteringVoice.GetVoiceDetails(masteringVoiceDetails);
        this.masteringVoiceChannels = masteringVoiceDetails.InputChannels;

        try {
            for (Map.Entry<String, byte[]> entry : soundData.entrySet()) {
                this.soundBuffers.put(entry.getKey(), this.loadAudioFile(entry.getValue()));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Could not load sound samples", e);
        }

        Runtime.getRuntime().addShutdownHook(this.shutdownHook = new Thread(() -> {
            this.shutdownHook = null;
            this.close();
        }));

        System.out.println("Initialized XAudio2 v2.9");
    }

    @Override
    public synchronized void playSound(final String sound, final float pitch, final float volume, final float panning) {
        if (!this.soundBuffers.containsKey(sound)) return;
        final SoundBuffer buffer = this.soundBuffers.get(sound);

        if (this.playingVoices.size() >= this.maxSounds) {
            this.playingVoices.remove(0).DestroyVoice();
        }

        final PointerByReference ppSourceVoice = new PointerByReference();
        this.checkError(this.xAudio2.CreateSourceVoice(ppSourceVoice, buffer.waveFormat, 0, 10F, null, null, null), "Could not create source voice");
        final XAudio2Library.XAudio2SourceVoice sourceVoice = new XAudio2Library.XAudio2SourceVoice(ppSourceVoice.getValue());

        this.checkError(sourceVoice.SubmitSourceBuffer(buffer.buffer, null), "Could not submit source buffer");
        this.checkError(sourceVoice.SetFrequencyRatio(pitch, XAudio2Library.XAUDIO2_COMMIT_NOW), "Could not frequency ratio");
        this.checkError(sourceVoice.SetVolume(volume, XAudio2Library.XAUDIO2_COMMIT_NOW), "Could not set volume");
        this.checkError(sourceVoice.SetOutputMatrix(masteringVoice, 1, this.masteringVoiceChannels, this.createPanMatrix(panning), XAudio2Library.XAUDIO2_COMMIT_NOW), "Could not set output matrix");
        this.checkError(sourceVoice.Start(0, XAudio2Library.XAUDIO2_COMMIT_NOW), "Could not start source voice");
        this.playingVoices.add(sourceVoice);
    }

    @Override
    public synchronized void preTick() {
        final XAudio2Library.XAUDIO2_VOICE_STATE.ByReference voiceState = new XAudio2Library.XAUDIO2_VOICE_STATE.ByReference();
        this.playingVoices.removeIf(voice -> {
            voice.GetState(voiceState, XAudio2Library.XAUDIO2_VOICE_NOSAMPLESPLAYED);
            if (voiceState.BuffersQueued == 0) {
                voice.DestroyVoice();
                return true;
            }
            return false;
        });
    }

    @Override
    public synchronized void stopSounds() {
        for (XAudio2Library.XAudio2SourceVoice voice : this.playingVoices) {
            voice.DestroyVoice();
        }
        this.playingVoices.clear();
    }

    @Override
    public synchronized void close() {
        if (this.shutdownHook != null) {
            Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
            this.shutdownHook = null;
        }
        if (this.xAudio2 != null) {
            this.xAudio2.Release(); // Release before deleting buffers
            this.xAudio2 = null;
        }
        this.masteringVoice = null;
        this.masteringVoiceChannelMask = 0;
        this.masteringVoiceChannels = 0;
        this.soundBuffers.clear();
        this.playingVoices.clear();
        Ole32.INSTANCE.CoUninitialize();
    }

    @Override
    public synchronized String getStatusLine() {
        return "Sounds: " + this.playingVoices.size() + " / " + this.maxSounds;
    }

    @Override
    public synchronized void setMasterVolume(final float volume) {
        this.checkError(this.masteringVoice.SetVolume(volume, XAudio2Library.XAUDIO2_COMMIT_NOW), "Could not set master volume");
    }

    private SoundBuffer loadAudioFile(final byte[] data) {
        try {
            final AudioInputStream audioInputStream = SoundFileUtil.readAudioFile(new ByteArrayInputStream(data));
            final AudioFormat audioFormat = audioInputStream.getFormat();
            final byte[] audioBytes = IOUtil.readFully(audioInputStream);

            final XAudio2Library.WAVEFORMATEX.ByReference waveFormat = new XAudio2Library.WAVEFORMATEX.ByReference();
            waveFormat.wFormatTag = XAudio2Library.WAVE_FORMAT_PCM;
            waveFormat.nChannels = (short) audioFormat.getChannels();
            waveFormat.nSamplesPerSec = (int) audioFormat.getSampleRate();
            waveFormat.wBitsPerSample = (short) audioFormat.getSampleSizeInBits();
            waveFormat.nBlockAlign = (short) audioFormat.getFrameSize();
            waveFormat.nAvgBytesPerSec = waveFormat.nSamplesPerSec * waveFormat.nBlockAlign;

            final XAudio2Library.XAUDIO2_BUFFER.ByReference buffer = new XAudio2Library.XAUDIO2_BUFFER.ByReference();
            buffer.Flags = XAudio2Library.XAUDIO2_END_OF_STREAM;
            buffer.AudioBytes = audioBytes.length;
            buffer.pAudioData = new Memory(buffer.AudioBytes);
            buffer.pAudioData.write(0, audioBytes, 0, audioBytes.length);

            return new SoundBuffer(buffer, waveFormat);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load audio file", e);
        }
    }

    private float[] createPanMatrix(final float pan) {
        final float left = 0.5F - pan / 2F;
        final float right = 0.5F + pan / 2F;
        final float[] outputMatrix = new float[8];

        switch (this.masteringVoiceChannelMask) {
            case XAudio2Library.SPEAKER_MONO:
                outputMatrix[0] = 1F;
                break;
            case XAudio2Library.SPEAKER_STEREO:
            case XAudio2Library.SPEAKER_2POINT1:
            case XAudio2Library.SPEAKER_SURROUND:
                outputMatrix[0] = left;
                outputMatrix[1] = right;
                break;
            case XAudio2Library.SPEAKER_QUAD:
                outputMatrix[0] = outputMatrix[2] = left;
                outputMatrix[1] = outputMatrix[3] = right;
                break;
            case XAudio2Library.SPEAKER_4POINT1:
                outputMatrix[0] = outputMatrix[3] = left;
                outputMatrix[1] = outputMatrix[4] = right;
                break;
            case XAudio2Library.SPEAKER_5POINT1:
            case XAudio2Library.SPEAKER_7POINT1:
            case XAudio2Library.SPEAKER_5POINT1_SURROUND:
                outputMatrix[0] = outputMatrix[4] = left;
                outputMatrix[1] = outputMatrix[5] = right;
                break;
            case XAudio2Library.SPEAKER_7POINT1_SURROUND:
                outputMatrix[0] = outputMatrix[4] = outputMatrix[6] = left;
                outputMatrix[1] = outputMatrix[5] = outputMatrix[7] = right;
                break;
        }

        return outputMatrix;
    }

    private void checkError(final int result, final String message, final int... allowedErrors) {
        if (result < 0) {
            for (int ignoreError : allowedErrors) {
                if (result == ignoreError) {
                    return;
                }
            }

            throw new RuntimeException("XAudio2 error: " + message + " (" + result + ")");
        }
    }

    private static class SoundBuffer {

        private final XAudio2Library.XAUDIO2_BUFFER.ByReference buffer;
        private final XAudio2Library.WAVEFORMATEX.ByReference waveFormat;

        public SoundBuffer(final XAudio2Library.XAUDIO2_BUFFER.ByReference buffer, final XAudio2Library.WAVEFORMATEX.ByReference waveFormat) {
            this.buffer = buffer;
            this.waveFormat = waveFormat;
        }

    }

}
