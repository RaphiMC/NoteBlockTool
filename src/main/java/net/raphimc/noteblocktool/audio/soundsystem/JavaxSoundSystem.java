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

import com.google.common.io.ByteStreams;
import com.google.common.io.LittleEndianDataOutputStream;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.raphimc.noteblocklib.util.Instrument;
import net.raphimc.noteblocktool.audio.SoundMap;
import net.raphimc.noteblocktool.util.SoundSampleUtil;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class JavaxSoundSystem {

    private static final Map<Instrument, Sound> SOUNDS = new HashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Javax Sound System").setDaemon(true).build());
    private static final List<Clip> PLAYING_SOUNDS = new CopyOnWriteArrayList<>();
    private static int MAX_SOUNDS = 256;
    private static ScheduledFuture<?> TICK_TASK;

    public static void init(final int maxSounds) {
        MAX_SOUNDS = maxSounds;
        try {
            for (Map.Entry<Instrument, String> entry : SoundMap.SOUNDS.entrySet()) {
                SOUNDS.put(entry.getKey(), readSound(JavaxSoundSystem.class.getResourceAsStream(entry.getValue())));
            }
        } catch (Throwable e) {
            throw new RuntimeException("Could not load audio buffer", e);
        }

        TICK_TASK = SCHEDULER.scheduleAtFixedRate(JavaxSoundSystem::tick, 0, 100, TimeUnit.MILLISECONDS);
    }

    public static int getMaxSounds() {
        return MAX_SOUNDS;
    }

    public static void playNote(final Instrument instrument, final float volume, final float pitch) {
        if (PLAYING_SOUNDS.size() >= MAX_SOUNDS) return;
        try {
            final Sound sound = SOUNDS.get(instrument);
            final int[] samples = SoundSampleUtil.mutate(sound.getSamples(), volume, pitch);
            final Sound newSound = new Sound(sound.getAudioFormat(), samples);
            final AudioInputStream audioStream = writeSound(newSound);
            final Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
            PLAYING_SOUNDS.add(clip);
        } catch (Throwable t) {
            throw new RuntimeException("Could not play note", t);
        }
    }

    public static void stopAllSounds() {
        final List<Clip> playingSounds = new ArrayList<>(PLAYING_SOUNDS);
        PLAYING_SOUNDS.clear();
        final Thread thread = new Thread(() -> {
            for (Clip clip : playingSounds) {
                clip.stop();
                clip.close();
            }
        });
        thread.start();
    }

    public static void destroy() {
        if (TICK_TASK != null) {
            TICK_TASK.cancel(true);
            TICK_TASK = null;
        }
        stopAllSounds();
        SOUNDS.clear();
    }

    public static int getPlayingSounds() {
        return PLAYING_SOUNDS.size();
    }

    private static void tick() {
        PLAYING_SOUNDS.removeIf(clip -> {
            if (clip.isRunning()) {
                return false;
            } else {
                clip.close();
                return true;
            }
        });
    }

    private static Sound readSound(final InputStream is) throws UnsupportedAudioFileException, IOException {
        final AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
        final AudioFormat audioFormat = in.getFormat();
        final byte[] audioBytes = ByteStreams.toByteArray(in);

        final int sampleSize = audioFormat.getSampleSizeInBits() / 8;
        final int[] samples = new int[audioBytes.length / sampleSize];
        for (int i = 0; i < samples.length; i++) {
            final byte[] sampleBytes = new byte[sampleSize];
            System.arraycopy(audioBytes, i * sampleSize, sampleBytes, 0, sampleSize);
            samples[i] = ByteBuffer.wrap(sampleBytes).order(ByteOrder.LITTLE_ENDIAN).getShort();
        }

        return new Sound(audioFormat, samples);
    }

    private static AudioInputStream writeSound(final Sound sound) throws IOException {
        final int sampleSize = sound.getAudioFormat().getSampleSizeInBits() / 8;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(sound.getSamples().length * sampleSize);
        final LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(baos);
        for (int sample : sound.getSamples()) {
            dos.writeShort((short) sample);
        }
        return new AudioInputStream(new ByteArrayInputStream(baos.toByteArray()), sound.getAudioFormat(), baos.size() / sound.getAudioFormat().getFrameSize());
    }


    private static final class Sound {
        private AudioFormat audioFormat;
        private int[] samples;

        private Sound(final AudioFormat audioFormat, final int[] samples) {
            this.audioFormat = audioFormat;
            this.samples = samples;
        }

        public AudioFormat getAudioFormat() {
            return this.audioFormat;
        }

        public void setAudioFormat(final AudioFormat audioFormat) {
            this.audioFormat = audioFormat;
        }

        public int[] getSamples() {
            return this.samples;
        }

        public void setSamples(final int[] samples) {
            this.samples = samples;
        }
    }

}
