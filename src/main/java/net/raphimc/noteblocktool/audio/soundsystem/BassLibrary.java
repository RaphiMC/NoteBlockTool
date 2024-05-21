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

import com.sun.jna.*;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.HashMap;
import java.util.Map;

public interface BassLibrary extends Library {

    BassLibrary INSTANCE = loadNative();

    int BASSVERSION = 0x204;

    // BASS_Init flags
    int BASS_DEVICE_8BITS = 1; // unused
    int BASS_DEVICE_MONO = 2; // mono
    int BASS_DEVICE_3D = 4; // unused
    int BASS_DEVICE_16BITS = 8; // limit output to 16-bit
    int BASS_DEVICE_REINIT = 128; // reinitialize
    int BASS_DEVICE_LATENCY = 0x100; // unused
    int BASS_DEVICE_CPSPEAKERS = 0x400; // unused
    int BASS_DEVICE_SPEAKERS = 0x800; // force enabling of speaker assignment
    int BASS_DEVICE_NOSPEAKER = 0x1000; // ignore speaker arrangement
    int BASS_DEVICE_DMIX = 0x2000; // use ALSA "dmix" plugin
    int BASS_DEVICE_FREQ = 0x4000; // set device sample rate
    int BASS_DEVICE_STEREO = 0x8000; // limit output to stereo
    int BASS_DEVICE_HOG = 0x10000; // hog/exclusive mode
    int BASS_DEVICE_AUDIOTRACK = 0x20000; // use AudioTrack output
    int BASS_DEVICE_DSOUND = 0x40000; // use DirectSound output
    int BASS_DEVICE_SOFTWARE = 0x80000; // disable hardware/fastpath output

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
    int BASS_CONFIG_VISTA_TRUEPOS = 30;
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
    int BASS_CONFIG_MF_VIDEO = 48;
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
    int BASS_ATTRIB_MUSIC_AMPLIFY = 0x100;
    int BASS_ATTRIB_MUSIC_PANSEP = 0x101;
    int BASS_ATTRIB_MUSIC_PSCALER = 0x102;
    int BASS_ATTRIB_MUSIC_BPM = 0x103;
    int BASS_ATTRIB_MUSIC_SPEED = 0x104;
    int BASS_ATTRIB_MUSIC_VOL_GLOBAL = 0x105;
    int BASS_ATTRIB_MUSIC_ACTIVE = 0x106;
    int BASS_ATTRIB_MUSIC_VOL_CHAN = 0x200; // + channel #
    int BASS_ATTRIB_MUSIC_VOL_INST = 0x300; // + instrument #

    // BASS_SampleGetChannel flags
    int BASS_SAMCHAN_NEW = 1; // get a new playback channel
    int BASS_SAMCHAN_STREAM = 2; // create a stream

    // BASS_ChannelSetSync types
    int BASS_SYNC_POS = 0;
    int BASS_SYNC_END = 2;
    int BASS_SYNC_META = 4;
    int BASS_SYNC_SLIDE = 5;
    int BASS_SYNC_STALL = 6;
    int BASS_SYNC_DOWNLOAD = 7;
    int BASS_SYNC_FREE = 8;
    int BASS_SYNC_SETPOS = 11;
    int BASS_SYNC_MUSICPOS = 10;
    int BASS_SYNC_MUSICINST = 1;
    int BASS_SYNC_MUSICFX = 3;
    int BASS_SYNC_OGG_CHANGE = 12;
    int BASS_SYNC_DEV_FAIL = 14;
    int BASS_SYNC_DEV_FORMAT = 15;
    int BASS_SYNC_THREAD = 0x20000000; // flag: call sync in other thread
    int BASS_SYNC_MIXTIME = 0x40000000; // flag: sync at mixtime, else at playtime
    int BASS_SYNC_ONETIME = 0x80000000; // flag: sync only once, else continuously

    int BASS_STREAM_DECODE = 0x200000; // don't play the stream, only decode
    int BASS_STREAM_AUTOFREE = 0x40000; // automatically free the stream when it stops/ends

    static BassLibrary loadNative() {
        try {
            final Map<String, Object> options = new HashMap<>();
            options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
            return Native.load("bass", BassLibrary.class, options);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    boolean BASS_Init(final int device, final int freq, final int flags, final int win, final PointerByReference clsid);

    boolean BASS_Free();

    boolean BASS_Start();

    boolean BASS_Stop();

    int BASS_GetVersion();

    int BASS_GetDevice();

    boolean BASS_GetDeviceInfo(final int device, final BASS_DEVICEINFO.ByReference info);

    int BASS_ErrorGetCode();

    float BASS_GetCPU();

    int BASS_GetConfig(final int option);

    boolean BASS_SetConfig(final int option, final int value);

    int BASS_SampleCreate(final int length, final int freq, final int chans, final int max, final int flags);

    boolean BASS_SampleSetData(final int handle, final byte[] buffer);

    int BASS_SampleGetChannel(final int handle, final int flags);

    boolean BASS_ChannelStart(final int handle);

    boolean BASS_ChannelGetAttribute(final int handle, final int attrib, final FloatByReference value);

    boolean BASS_ChannelSetAttribute(final int handle, final int attrib, final float value);

    int BASS_ChannelSetSync(final int handle, final int type, final long param, final SYNCPROC proc, final Pointer user);

    boolean BASS_ChannelStop(final int handle);

    interface SYNCPROC extends Callback {

        void syncProc(final int handle, final int channel, final int data, final Pointer user);

    }

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
