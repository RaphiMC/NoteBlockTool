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
package net.raphimc.noteblocktool.audio.soundsystem;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import net.raphimc.noteblocktool.util.jna.COMInvoker;
import net.raphimc.noteblocktool.util.jna.COMObject;

import java.util.HashMap;
import java.util.Map;

public interface XAudio2Library extends Library {

    XAudio2Library INSTANCE = loadNative();

    int XAUDIO2_ANY_PROCESSOR = 0xFFFFFFFF;
    int XAUDIO2_DEFAULT_CHANNELS = 0;
    int XAUDIO2_DEFAULT_SAMPLERATE = 0;
    int WAVE_FORMAT_PCM = 1;
    int XAUDIO2_END_OF_STREAM = 0x40;
    int XAUDIO2_COMMIT_NOW = 0;

    int SPEAKER_FRONT_LEFT = 0x00000001;
    int SPEAKER_FRONT_RIGHT = 0x00000002;
    int SPEAKER_FRONT_CENTER = 0x00000004;
    int SPEAKER_LOW_FREQUENCY = 0x00000008;
    int SPEAKER_BACK_LEFT = 0x00000010;
    int SPEAKER_BACK_RIGHT = 0x00000020;
    int SPEAKER_FRONT_LEFT_OF_CENTER = 0x00000040;
    int SPEAKER_FRONT_RIGHT_OF_CENTER = 0x00000080;
    int SPEAKER_BACK_CENTER = 0x00000100;
    int SPEAKER_SIDE_LEFT = 0x00000200;
    int SPEAKER_SIDE_RIGHT = 0x00000400;
    int SPEAKER_TOP_CENTER = 0x00000800;
    int SPEAKER_TOP_FRONT_LEFT = 0x00001000;
    int SPEAKER_TOP_FRONT_CENTER = 0x00002000;
    int SPEAKER_TOP_FRONT_RIGHT = 0x00004000;
    int SPEAKER_TOP_BACK_LEFT = 0x00008000;
    int SPEAKER_TOP_BACK_CENTER = 0x00010000;
    int SPEAKER_TOP_BACK_RIGHT = 0x00020000;
    int SPEAKER_MONO = SPEAKER_FRONT_CENTER;
    int SPEAKER_STEREO = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT;
    int SPEAKER_2POINT1 = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_LOW_FREQUENCY;
    int SPEAKER_SURROUND = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_FRONT_CENTER | SPEAKER_BACK_CENTER;
    int SPEAKER_QUAD = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_BACK_LEFT | SPEAKER_BACK_RIGHT;
    int SPEAKER_4POINT1 = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_LOW_FREQUENCY | SPEAKER_BACK_LEFT | SPEAKER_BACK_RIGHT;
    int SPEAKER_5POINT1 = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_FRONT_CENTER | SPEAKER_LOW_FREQUENCY | SPEAKER_BACK_LEFT | SPEAKER_BACK_RIGHT;
    int SPEAKER_7POINT1 = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_FRONT_CENTER | SPEAKER_LOW_FREQUENCY | SPEAKER_BACK_LEFT | SPEAKER_BACK_RIGHT | SPEAKER_FRONT_LEFT_OF_CENTER | SPEAKER_FRONT_RIGHT_OF_CENTER;
    int SPEAKER_5POINT1_SURROUND = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_FRONT_CENTER | SPEAKER_LOW_FREQUENCY | SPEAKER_SIDE_LEFT | SPEAKER_SIDE_RIGHT;
    int SPEAKER_7POINT1_SURROUND = SPEAKER_FRONT_LEFT | SPEAKER_FRONT_RIGHT | SPEAKER_FRONT_CENTER | SPEAKER_LOW_FREQUENCY | SPEAKER_BACK_LEFT | SPEAKER_BACK_RIGHT | SPEAKER_SIDE_LEFT | SPEAKER_SIDE_RIGHT;

    static XAudio2Library loadNative() {
        final Map<String, Object> options = new HashMap<>();
        options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
        try {
            return Native.load("XAudio2_9", XAudio2Library.class, options);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    long XAudio2Create(final PointerByReference ppXAudio2, final int Flags, final int XAudio2Processor);

    class XAudio2 extends COMObject {

        public XAudio2() {
        }

        public XAudio2(final Pointer pvInstance) {
            super(pvInstance);
        }

        public long CreateSourceVoice(final PointerByReference ppSourceVoice, final WAVEFORMATEX.ByReference pSourceFormat, final int Flags, final float MaxFrequencyRatio, final Pointer pCallback, final Pointer pSendList, final Pointer pEffectChain) {
            return this.getVtableFunction(5).invokeLong(new Object[]{this.getPointer(), ppSourceVoice, pSourceFormat, Flags, MaxFrequencyRatio, pCallback, pSendList, pEffectChain});
        }

        public long CreateMasteringVoice(final PointerByReference ppMasteringVoice, final int InputChannels, final int InputSampleRate, final int Flags, final String szDeviceId, final Pointer pEffectChain, final int StreamCategory) {
            return this.getVtableFunction(7).invokeLong(new Object[]{this.getPointer(), ppMasteringVoice, InputChannels, InputSampleRate, Flags, szDeviceId, pEffectChain, StreamCategory});
        }

        public void GetPerformanceData(final XAUDIO2_PERFORMANCE_DATA.ByReference pPerfData) {
            this.getVtableFunction(11).invokeVoid(new Object[]{this.getPointer(), pPerfData});
        }

    }

    class XAudio2Voice extends COMInvoker {

        public XAudio2Voice() {
        }

        public XAudio2Voice(final Pointer pvInstance) {
            super(pvInstance);
        }

        public void GetVoiceDetails(final XAUDIO2_VOICE_DETAILS.ByReference pVoiceDetails) {
            this.getVtableFunction(0).invokeVoid(new Object[]{this.getPointer(), pVoiceDetails});
        }

        public long SetVolume(final float Volume, final int OperationSet) {
            return this.getVtableFunction(12).invokeLong(new Object[]{this.getPointer(), Volume, OperationSet});
        }

        public long SetOutputMatrix(final XAudio2Voice pDestinationVoice, final int SourceChannels, final int DestinationChannels, final float[] pLevelMatrix, final int OperationSet) {
            return this.getVtableFunction(16).invokeLong(new Object[]{this.getPointer(), pDestinationVoice.getPointer(), SourceChannels, DestinationChannels, pLevelMatrix, OperationSet});
        }

        public void DestroyVoice() {
            this.getVtableFunction(18).invokeVoid(new Object[]{this.getPointer()});
        }

    }

    class XAudio2MasteringVoice extends XAudio2Voice {

        public XAudio2MasteringVoice() {
        }

        public XAudio2MasteringVoice(final Pointer pvInstance) {
            super(pvInstance);
        }

        public long GetChannelMask(final IntByReference pChannelMask) {
            return this.getVtableFunction(19).invokeLong(new Object[]{this.getPointer(), pChannelMask});
        }

    }

    class XAudio2SourceVoice extends XAudio2Voice {

        public XAudio2SourceVoice() {
        }

        public XAudio2SourceVoice(final Pointer pvInstance) {
            super(pvInstance);
        }

        public long Start(final int Flags, final int OperationSet) {
            return this.getVtableFunction(19).invokeLong(new Object[]{this.getPointer(), Flags, OperationSet});
        }

        public long Stop(final int Flags, final int OperationSet) {
            return this.getVtableFunction(20).invokeLong(new Object[]{this.getPointer(), Flags, OperationSet});
        }

        public long SubmitSourceBuffer(final XAUDIO2_BUFFER.ByReference pBuffer, final Pointer pBufferWMA) {
            return this.getVtableFunction(21).invokeLong(new Object[]{this.getPointer(), pBuffer, pBufferWMA});
        }

        public long FlushSourceBuffers() {
            return this.getVtableFunction(22).invokeLong(new Object[]{this.getPointer()});
        }

        public void GetState(final XAUDIO2_VOICE_STATE.ByReference pVoiceState, final int Flags) {
            this.getVtableFunction(25).invokeVoid(new Object[]{this.getPointer(), pVoiceState, Flags});
        }

        public long SetFrequencyRatio(final float Ratio, final int OperationSet) {
            return this.getVtableFunction(26).invokeLong(new Object[]{this.getPointer(), Ratio, OperationSet});
        }

        public long SetSourceSampleRate(final int NewSourceSampleRate) {
            return this.getVtableFunction(28).invokeLong(new Object[]{this.getPointer(), NewSourceSampleRate});
        }

    }

    @Structure.FieldOrder({"AudioCyclesSinceLastQuery", "TotalCyclesSinceLastQuery", "MinimumCyclesPerQuantum", "MaximumCyclesPerQuantum", "MemoryUsageInBytes", "CurrentLatencyInSamples", "GlitchesSinceEngineStarted", "ActiveSourceVoiceCount", "TotalSourceVoiceCount", "ActiveSubmixVoiceCount", "ActiveResamplerCount", "ActiveMatrixMixCount", "ActiveXmaSourceVoices", "ActiveXmaStreams"})
    class XAUDIO2_PERFORMANCE_DATA extends Structure {

        public long AudioCyclesSinceLastQuery;
        public long TotalCyclesSinceLastQuery;
        public int MinimumCyclesPerQuantum;
        public int MaximumCyclesPerQuantum;
        public int MemoryUsageInBytes;
        public int CurrentLatencyInSamples;
        public int GlitchesSinceEngineStarted;
        public int ActiveSourceVoiceCount;
        public int TotalSourceVoiceCount;
        public int ActiveSubmixVoiceCount;
        public int ActiveResamplerCount;
        public int ActiveMatrixMixCount;
        public int ActiveXmaSourceVoices;
        public int ActiveXmaStreams;

        public XAUDIO2_PERFORMANCE_DATA() {
        }

        public static class ByReference extends XAUDIO2_PERFORMANCE_DATA implements Structure.ByReference {
        }

        public static class ByValue extends XAUDIO2_PERFORMANCE_DATA implements Structure.ByValue {
        }

    }

    @Structure.FieldOrder({"wFormatTag", "nChannels", "nSamplesPerSec", "nAvgBytesPerSec", "nBlockAlign", "wBitsPerSample", "cbSize"})
    class WAVEFORMATEX extends Structure {

        public short wFormatTag;
        public short nChannels;
        public int nSamplesPerSec;
        public int nAvgBytesPerSec;
        public short nBlockAlign;
        public short wBitsPerSample;
        public short cbSize;

        public WAVEFORMATEX() {
        }

        public static class ByReference extends WAVEFORMATEX implements Structure.ByReference {
        }

        public static class ByValue extends WAVEFORMATEX implements Structure.ByValue {
        }

    }

    @Structure.FieldOrder({"Flags", "AudioBytes", "pAudioData", "PlayBegin", "PlayLength", "LoopBegin", "LoopLength", "LoopCount", "pContext"})
    class XAUDIO2_BUFFER extends Structure {

        public int Flags;
        public int AudioBytes;
        public Pointer pAudioData;
        public int PlayBegin;
        public int PlayLength;
        public int LoopBegin;
        public int LoopLength;
        public int LoopCount;
        public Pointer pContext;

        public XAUDIO2_BUFFER() {
        }

        public static class ByReference extends XAUDIO2_BUFFER implements Structure.ByReference {
        }

        public static class ByValue extends XAUDIO2_BUFFER implements Structure.ByValue {
        }

    }

    @Structure.FieldOrder({"pCurrentBufferContext", "BuffersQueued", "SamplesPlayed"})
    class XAUDIO2_VOICE_STATE extends Structure {

        public Pointer pCurrentBufferContext;
        public int BuffersQueued;
        public long SamplesPlayed;

        public XAUDIO2_VOICE_STATE() {
        }

        public static class ByReference extends XAUDIO2_VOICE_STATE implements Structure.ByReference {
        }

        public static class ByValue extends XAUDIO2_VOICE_STATE implements Structure.ByValue {
        }

    }

    @Structure.FieldOrder({"CreationFlags", "ActiveFlags", "InputChannels", "InputSampleRate"})
    class XAUDIO2_VOICE_DETAILS extends Structure {

        public int CreationFlags;
        public int ActiveFlags;
        public int InputChannels;
        public int InputSampleRate;

        public XAUDIO2_VOICE_DETAILS() {
        }

        public static class ByReference extends XAUDIO2_VOICE_DETAILS implements Structure.ByReference {
        }

        public static class ByValue extends XAUDIO2_VOICE_DETAILS implements Structure.ByValue {
        }

    }

}
