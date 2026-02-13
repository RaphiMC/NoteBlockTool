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
package net.raphimc.noteblocktool.audio.library;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.HashMap;
import java.util.Map;

public interface BassLibrary extends Library {

    BassLibrary INSTANCE = loadNative();

    int BASSVERSION = 0x204;

    // Error codes returned by BASS_ErrorGetCode
    int BASS_OK = 0; // all is OK
    int BASS_ERROR_MEM = 1; // memory error
    int BASS_ERROR_FILEOPEN = 2; // can't open the file
    int BASS_ERROR_DRIVER = 3; // can't find a free/valid driver
    int BASS_ERROR_BUFLOST = 4; // the sample buffer was lost
    int BASS_ERROR_HANDLE = 5; // invalid handle
    int BASS_ERROR_FORMAT = 6; // unsupported sample format
    int BASS_ERROR_POSITION = 7; // invalid position
    int BASS_ERROR_INIT = 8; // BASS_Init has not been successfully called
    int BASS_ERROR_START = 9; // BASS_Start has not been successfully called
    int BASS_ERROR_SSL = 10; // SSL/HTTPS support isn't available
    int BASS_ERROR_REINIT = 11; // device needs to be reinitialized
    int BASS_ERROR_TRACK = 13; // invalid track number
    int BASS_ERROR_ALREADY = 14; // already initialized/paused/whatever
    int BASS_ERROR_NOTAUDIO = 17; // file does not contain audio
    int BASS_ERROR_NOCHAN = 18; // can't get a free channel
    int BASS_ERROR_ILLTYPE = 19; // an illegal type was specified
    int BASS_ERROR_ILLPARAM = 20; // an illegal parameter was specified
    int BASS_ERROR_NO3D = 21; // no 3D support
    int BASS_ERROR_NOEAX = 22; // no EAX support
    int BASS_ERROR_DEVICE = 23; // illegal device number
    int BASS_ERROR_NOPLAY = 24; // not playing
    int BASS_ERROR_FREQ = 25; // illegal sample rate
    int BASS_ERROR_NOTFILE = 27; // the stream is not a file stream
    int BASS_ERROR_NOHW = 29; // no hardware voices available
    int BASS_ERROR_EMPTY = 31; // the file has no sample data
    int BASS_ERROR_NONET = 32; // no internet connection could be opened
    int BASS_ERROR_CREATE = 33; // couldn't create the file
    int BASS_ERROR_NOFX = 34; // effects are not available
    int BASS_ERROR_NOTAVAIL = 37; // requested data/action is not available
    int BASS_ERROR_DECODE = 38; // the channel is/isn't a "decoding channel"
    int BASS_ERROR_DX = 39; // a sufficient DirectX version is not installed
    int BASS_ERROR_TIMEOUT = 40; // connection timedout
    int BASS_ERROR_FILEFORM = 41; // unsupported file format
    int BASS_ERROR_SPEAKER = 42; // unavailable speaker
    int BASS_ERROR_VERSION = 43; // invalid BASS version (used by add-ons)
    int BASS_ERROR_CODEC = 44; // codec is not available/supported
    int BASS_ERROR_ENDED = 45; // the channel/file has ended
    int BASS_ERROR_BUSY = 46; // the device is busy
    int BASS_ERROR_UNSTREAMABLE = 47; // unstreamable file
    int BASS_ERROR_PROTOCOL = 48; // unsupported protocol
    int BASS_ERROR_DENIED = 49; // access denied
    int BASS_ERROR_FREEING = 50; // being freed
    int BASS_ERROR_CANCEL = 51; // cancelled
    int BASS_ERROR_UNKNOWN = -1; // some other mystery problem

    // BASS_SetConfig options
    int BASS_CONFIG_BUFFER = 0;
    int BASS_CONFIG_UPDATEPERIOD = 1;
    int BASS_CONFIG_GVOL_SAMPLE = 4;
    int BASS_CONFIG_GVOL_STREAM = 5;
    int BASS_CONFIG_GVOL_MUSIC = 6;
    int BASS_CONFIG_CURVE_VOL = 7;
    int BASS_CONFIG_CURVE_PAN = 8;
    int BASS_CONFIG_FLOATDSP = 9;
    int BASS_CONFIG_3DALGORITHM = 10;
    int BASS_CONFIG_NET_TIMEOUT = 11;
    int BASS_CONFIG_NET_BUFFER = 12;
    int BASS_CONFIG_PAUSE_NOPLAY = 13;
    int BASS_CONFIG_NET_PREBUF = 15;
    int BASS_CONFIG_NET_PASSIVE = 18;
    int BASS_CONFIG_REC_BUFFER = 19;
    int BASS_CONFIG_NET_PLAYLIST = 21;
    int BASS_CONFIG_MUSIC_VIRTUAL = 22;
    int BASS_CONFIG_VERIFY = 23;
    int BASS_CONFIG_UPDATETHREADS = 24;
    int BASS_CONFIG_DEV_BUFFER = 27;
    int BASS_CONFIG_REC_LOOPBACK = 28;
    int BASS_CONFIG_IOS_SESSION = 34;
    int BASS_CONFIG_IOS_MIXAUDIO = 34;
    int BASS_CONFIG_DEV_DEFAULT = 36;
    int BASS_CONFIG_NET_READTIMEOUT = 37;
    int BASS_CONFIG_VISTA_SPEAKERS = 38;
    int BASS_CONFIG_IOS_SPEAKER = 39;
    int BASS_CONFIG_MF_DISABLE = 40;
    int BASS_CONFIG_HANDLES = 41;
    int BASS_CONFIG_UNICODE = 42;
    int BASS_CONFIG_SRC = 43;
    int BASS_CONFIG_SRC_SAMPLE = 44;
    int BASS_CONFIG_ASYNCFILE_BUFFER = 45;
    int BASS_CONFIG_OGG_PRESCAN = 47;
    int BASS_CONFIG_VIDEO = 48;
    int BASS_CONFIG_AIRPLAY = 49;
    int BASS_CONFIG_DEV_NONSTOP = 50;
    int BASS_CONFIG_IOS_NOCATEGORY = 51;
    int BASS_CONFIG_VERIFY_NET = 52;
    int BASS_CONFIG_DEV_PERIOD = 53;
    int BASS_CONFIG_FLOAT = 54;
    int BASS_CONFIG_NET_SEEK = 56;
    int BASS_CONFIG_AM_DISABLE = 58;
    int BASS_CONFIG_NET_PLAYLIST_DEPTH = 59;
    int BASS_CONFIG_NET_PREBUF_WAIT = 60;
    int BASS_CONFIG_ANDROID_SESSIONID = 62;
    int BASS_CONFIG_WASAPI_PERSIST = 65;
    int BASS_CONFIG_REC_WASAPI = 66;
    int BASS_CONFIG_ANDROID_AAUDIO = 67;
    int BASS_CONFIG_SAMPLE_ONEHANDLE = 69;
    int BASS_CONFIG_NET_META = 71;
    int BASS_CONFIG_NET_RESTRATE = 72;
    int BASS_CONFIG_REC_DEFAULT = 73;
    int BASS_CONFIG_NORAMP = 74;
    int BASS_CONFIG_NOSOUND_MAXDELAY = 76;
    int BASS_CONFIG_STACKALLOC = 79;
    int BASS_CONFIG_DOWNMIX = 80;

    int BASS_SAMPLE_8BITS = 1; // 8 bit
    int BASS_SAMPLE_MONO = 2; // mono
    int BASS_SAMPLE_LOOP = 4; // looped
    int BASS_SAMPLE_3D = 8; // 3D functionality
    int BASS_SAMPLE_SOFTWARE = 0x10; // unused
    int BASS_SAMPLE_MUTEMAX = 0x20; // mute at max distance (3D only)
    int BASS_SAMPLE_NOREORDER = 0x40; // don't reorder channels to match speakers
    int BASS_SAMPLE_FX = 0x80; // unused
    int BASS_SAMPLE_FLOAT = 0x100; // 32 bit floating-point
    int BASS_SAMPLE_OVER_VOL = 0x10000; // override lowest volume
    int BASS_SAMPLE_OVER_POS = 0x20000; // override longest playing
    int BASS_SAMPLE_OVER_DIST = 0x30000; // override furthest from listener (3D only)

    int BASS_STREAM_PRESCAN = 0x20000; // scan file for accurate seeking and length
    int BASS_STREAM_AUTOFREE = 0x40000; // automatically free the stream when it stops/ends
    int BASS_STREAM_RESTRATE = 0x80000; // restrict the download rate of internet file stream
    int BASS_STREAM_BLOCK = 0x100000; // download internet file stream in small blocks
    int BASS_STREAM_DECODE = 0x200000; // don't play the stream, only decode
    int BASS_STREAM_STATUS = 0x800000; // give server status info (HTTP/ICY tags) in DOWNLOADPROC

    // BASS_SampleGetChannel flags
    int BASS_SAMCHAN_NEW = 1; // get a new playback channel
    int BASS_SAMCHAN_STREAM = 2; // create a stream

    // BASS_ChannelIsActive return values
    int BASS_ACTIVE_STOPPED = 0;
    int BASS_ACTIVE_PLAYING = 1;
    int BASS_ACTIVE_STALLED = 2;
    int BASS_ACTIVE_PAUSED = 3;
    int BASS_ACTIVE_PAUSED_DEVICE = 4;

    // Channel attributes
    int BASS_ATTRIB_FREQ = 1;
    int BASS_ATTRIB_VOL = 2;
    int BASS_ATTRIB_PAN = 3;
    int BASS_ATTRIB_EAXMIX = 4;
    int BASS_ATTRIB_NOBUFFER = 5;
    int BASS_ATTRIB_VBR = 6;
    int BASS_ATTRIB_CPU = 7;
    int BASS_ATTRIB_SRC = 8;
    int BASS_ATTRIB_NET_RESUME = 9;
    int BASS_ATTRIB_SCANINFO = 10;
    int BASS_ATTRIB_NORAMP = 11;
    int BASS_ATTRIB_BITRATE = 12;
    int BASS_ATTRIB_BUFFER = 13;
    int BASS_ATTRIB_GRANULE = 14;
    int BASS_ATTRIB_USER = 15;
    int BASS_ATTRIB_TAIL = 16;
    int BASS_ATTRIB_PUSH_LIMIT = 17;
    int BASS_ATTRIB_DOWNLOADPROC = 18;
    int BASS_ATTRIB_VOLDSP = 19;
    int BASS_ATTRIB_VOLDSP_PRIORITY = 20;
    int BASS_ATTRIB_DOWNMIX = 21;
    int BASS_ATTRIB_MUSIC_AMPLIFY = 0x100;
    int BASS_ATTRIB_MUSIC_PANSEP = 0x101;
    int BASS_ATTRIB_MUSIC_PSCALER = 0x102;
    int BASS_ATTRIB_MUSIC_BPM = 0x103;
    int BASS_ATTRIB_MUSIC_SPEED = 0x104;
    int BASS_ATTRIB_MUSIC_VOL_GLOBAL = 0x105;
    int BASS_ATTRIB_MUSIC_ACTIVE = 0x106;
    int BASS_ATTRIB_MUSIC_VOL_CHAN = 0x200; // + channel #
    int BASS_ATTRIB_MUSIC_VOL_INST = 0x300; // + instrument #

    // BASS_ChannelGetData flags
    int BASS_DATA_AVAILABLE = 0; // query how much data is buffered
    int BASS_DATA_NOREMOVE = 0x10000000; // flag: don't remove data from recording buffer
    int BASS_DATA_FIXED = 0x20000000; // unused
    int BASS_DATA_FLOAT = 0x40000000; // flag: return floating-point sample data
    int BASS_DATA_FFT256 = 0x80000000; // 256 sample FFT
    int BASS_DATA_FFT512 = 0x80000001; // 512 FFT
    int BASS_DATA_FFT1024 = 0x80000002; // 1024 FFT
    int BASS_DATA_FFT2048 = 0x80000003; // 2048 FFT
    int BASS_DATA_FFT4096 = 0x80000004; // 4096 FFT
    int BASS_DATA_FFT8192 = 0x80000005; // 8192 FFT
    int BASS_DATA_FFT16384 = 0x80000006; // 16384 FFT
    int BASS_DATA_FFT32768 = 0x80000007; // 32768 FFT
    int BASS_DATA_FFT_INDIVIDUAL = 0x10; // FFT flag: FFT for each channel, else all combined
    int BASS_DATA_FFT_NOWINDOW = 0x20; // FFT flag: no Hanning window
    int BASS_DATA_FFT_REMOVEDC = 0x40; // FFT flag: pre-remove DC bias
    int BASS_DATA_FFT_COMPLEX = 0x80; // FFT flag: return complex data
    int BASS_DATA_FFT_NYQUIST = 0x100; // FFT flag: return extra Nyquist value

    static BassLibrary loadNative() {
        final Map<String, Object> options = new HashMap<>();
        options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
        try {
            return Native.load("bass", BassLibrary.class, options);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    boolean BASS_SetConfig(final int option, final int value);

    int BASS_GetVersion();

    int BASS_ErrorGetCode();

    boolean BASS_GetDeviceInfo(final int device, final BASS_DEVICEINFO.ByReference info);

    boolean BASS_Init(final int device, final int freq, final int flags, final int win, final PointerByReference clsid);

    boolean BASS_Free();

    int BASS_GetDevice();

    boolean BASS_Start();

    boolean BASS_Stop();

    float BASS_GetCPU();

    int BASS_SampleCreate(final int length, final int freq, final int chans, final int max, final int flags);

    boolean BASS_SampleSetData(final int handle, final byte[] buffer);

    int BASS_SampleGetChannel(final int handle, final int flags);

    int BASS_ChannelIsActive(final int handle);

    boolean BASS_ChannelFree(final int handle);

    boolean BASS_ChannelStart(final int handle);

    boolean BASS_ChannelSetAttribute(final int handle, final int attrib, final float value);

    boolean BASS_ChannelGetAttribute(final int handle, final int attrib, final FloatByReference value);

    int BASS_ChannelGetData(final int handle, final Pointer buffer, final int length);

    @Structure.FieldOrder({"name", "driver", "flags"})
    class BASS_DEVICEINFO extends Structure {

        public String name;
        public String driver;
        public int flags;

        public BASS_DEVICEINFO() {
        }

        public BASS_DEVICEINFO(final String name, final String driver, final int flags) {
            this.name = name;
            this.driver = driver;
            this.flags = flags;
        }

        public static class ByReference extends BASS_DEVICEINFO implements Structure.ByReference {
        }

        public static class ByValue extends BASS_DEVICEINFO implements Structure.ByValue {
        }

    }

}
