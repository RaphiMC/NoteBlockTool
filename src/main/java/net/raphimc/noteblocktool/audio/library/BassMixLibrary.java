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

import java.util.HashMap;
import java.util.Map;

public interface BassMixLibrary extends Library {

    BassMixLibrary INSTANCE = loadNative();

    // Additional BASS_SetConfig options
    int BASS_CONFIG_MIXER_BUFFER = 0x10601;
    int BASS_CONFIG_MIXER_POSEX = 0x10602;
    int BASS_CONFIG_SPLIT_BUFFER = 0x10610;

    // BASS_Mixer_StreamCreate flags
    int BASS_MIXER_RESUME = 0x1000;// resume stalled immediately upon new/unpaused source
    int BASS_MIXER_POSEX = 0x2000;// enable BASS_Mixer_ChannelGetPositionEx support
    int BASS_MIXER_NOSPEAKER = 0x4000;// ignore speaker arrangement
    int BASS_MIXER_QUEUE = 0x8000;// queue sources
    int BASS_MIXER_END = 0x10000;// end the stream when there are no sources
    int BASS_MIXER_NONSTOP = 0x20000;// don't stall when there are no sources

    // BASS_Mixer_StreamAddChannel/Ex flags
    int BASS_MIXER_CHAN_ABSOLUTE = 0x1000;// start is an absolute position
    int BASS_MIXER_CHAN_BUFFER = 0x2000;// buffer data for BASS_Mixer_ChannelGetData/Level
    int BASS_MIXER_CHAN_LIMIT = 0x4000;// limit mixer processing to the amount available from this source
    int BASS_MIXER_CHAN_MATRIX = 0x10000;// matrix mixing
    int BASS_MIXER_CHAN_PAUSE = 0x20000;// don't process the source
    int BASS_MIXER_CHAN_DOWNMIX = 0x400000; // downmix to stereo/mono
    int BASS_MIXER_CHAN_NORAMPIN = 0x800000; // don't ramp-in the start
    int BASS_MIXER_BUFFER = BASS_MIXER_CHAN_BUFFER;
    int BASS_MIXER_LIMIT = BASS_MIXER_CHAN_LIMIT;
    int BASS_MIXER_MATRIX = BASS_MIXER_CHAN_MATRIX;
    int BASS_MIXER_PAUSE = BASS_MIXER_CHAN_PAUSE;
    int BASS_MIXER_DOWNMIX = BASS_MIXER_CHAN_DOWNMIX;
    int BASS_MIXER_NORAMPIN = BASS_MIXER_CHAN_NORAMPIN;

    // Mixer attributes
    int BASS_ATTRIB_MIXER_LATENCY = 0x15000;
    int BASS_ATTRIB_MIXER_THREADS = 0x15001;
    int BASS_ATTRIB_MIXER_VOL = 0x15002;

    // Additional BASS_Mixer_ChannelIsActive return values
    int BASS_ACTIVE_WAITING = 5;
    int BASS_ACTIVE_QUEUED = 6;

    // BASS_Split_StreamCreate flags
    int BASS_SPLIT_SLAVE = 0x1000;// only read buffered data
    int BASS_SPLIT_POS = 0x2000;

    // Splitter attributes
    int BASS_ATTRIB_SPLIT_ASYNCBUFFER = 0x15010;
    int BASS_ATTRIB_SPLIT_ASYNCPERIOD = 0x15011;

    // Envelope types
    int BASS_MIXER_ENV_FREQ = 1;
    int BASS_MIXER_ENV_VOL = 2;
    int BASS_MIXER_ENV_PAN = 3;
    int BASS_MIXER_ENV_LOOP = 0x10000; // flag: loop
    int BASS_MIXER_ENV_REMOVE = 0x20000; // flag: remove at end

    // Additional sync types
    int BASS_SYNC_MIXER_ENVELOPE = 0x10200;
    int BASS_SYNC_MIXER_ENVELOPE_NODE = 0x10201;
    int BASS_SYNC_MIXER_QUEUE = 0x10202;

    // Additional BASS_Mixer_ChannelSetPosition flag
    int BASS_POS_MIXER_RESET = 0x10000; // flag: clear mixer's playback buffer

    // Additional BASS_Mixer_ChannelGetPosition mode
    int BASS_POS_MIXER_DELAY = 5;

    // BASS_CHANNELINFO types
    int BASS_CTYPE_STREAM_MIXER = 0x10800;
    int BASS_CTYPE_STREAM_SPLIT = 0x10801;

    static BassMixLibrary loadNative() {
        try {
            final Map<String, Object> options = new HashMap<>();
            options.put(Library.OPTION_STRING_ENCODING, "UTF-8");
            return Native.load("bassmix", BassMixLibrary.class, options);
        } catch (Throwable ignored) {
        }
        return null;
    }

    static boolean isLoaded() {
        return INSTANCE != null;
    }

    int BASS_Mixer_StreamCreate(final int freq, final int chans, final int flags);

    boolean BASS_Mixer_StreamAddChannel(final int handle, final int channel, final int flags);

}
