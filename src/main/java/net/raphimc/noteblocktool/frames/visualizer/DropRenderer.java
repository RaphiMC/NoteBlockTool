/*
 * This file is part of NoteBlockTool - https://github.com/RaphiMC/NoteBlockTool
 * Copyright (C) 2022-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.noteblocktool.frames.visualizer;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.lenni0451.commons.Sneaky;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.raphimc.noteblocklib.data.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.NbsDefinitions;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.Note;
import net.raphimc.noteblocklib.model.Song;
import net.raphimc.noteblocklib.util.SongUtil;
import net.raphimc.noteblocktool.audio.player.AudioSystemSongPlayer;
import net.raphimc.thingl.ThinGL;
import net.raphimc.thingl.renderer.impl.RendererText;
import net.raphimc.thingl.resource.image.texture.Texture2D;
import net.raphimc.thingl.text.TextRun;
import net.raphimc.thingl.text.font.Font;
import net.raphimc.thingl.text.shaping.ShapedTextRun;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11C;

import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class DropRenderer {

    private static final int TEXT_SIZE = 24;
    private static final int PIANO_KEY_COUNT = 88;
    private static final int BLACK_PIANO_KEY_COUNT = 36;
    private static final int WHITE_PIANO_KEY_COUNT = PIANO_KEY_COUNT - BLACK_PIANO_KEY_COUNT;
    private static final float BLACK_KEY_WIDTH_RATIO = 0.6F;
    private static final float BLACK_KEY_HEIGHT_RATIO = 0.6F;
    private static final float PIANO_HEIGHT_DIVIDER = 7F;
    private static final int KEY_LINE_HEIGHT = 2;
    private static final float WHITE_KEY_LINE_OFFSET_RATIO = 18F;
    private static final float BLACK_KEY_LINE_OFFSET_RATIO = 12F;
    private static final float KEY_PRESS_DEPTH_RATIO = 14.5F;
    private static final int PRESSED_KEY_COLOR_ALPHA = 175;
    private static final long KEY_ANIMATION_DURATION = 250_000_000L;

    private final AudioSystemSongPlayer songPlayer;
    private Font robotoFont;
    private Texture2D noteBlockTexture;
    private Map<MinecraftInstrument, Color> instrumentColors;
    private Map<NbsCustomInstrument, Pair<Color, Color>> customInstrumentColors;
    private float[] pianoKeyPositions;
    private long[] pianoKeyLastPlayed;
    private Color[] pianoKeyLastColors;
    private int renderedNotes;

    public DropRenderer(final AudioSystemSongPlayer songPlayer) {
        this.songPlayer = songPlayer;
    }

    public void init() {
        try {
            try (final InputStream stream = DropRenderer.class.getResourceAsStream("/fonts/Roboto-Regular.ttf")) {
                if (stream == null) {
                    throw new IllegalStateException("Failed to find Roboto font");
                }
                this.robotoFont = new Font(stream.readAllBytes(), TEXT_SIZE);
            }
            try (final InputStream stream = DropRenderer.class.getResourceAsStream("/textures/note_block.png")) {
                if (stream == null) {
                    throw new IllegalStateException("Failed to find note block texture");
                }
                this.noteBlockTexture = Texture2D.fromImage(stream.readAllBytes());
                this.noteBlockTexture.setFilter(GL11C.GL_NEAREST);
            }
        } catch (Throwable e) {
            Sneaky.sneak(e);
        }

        this.instrumentColors = new EnumMap<>(MinecraftInstrument.class);
        this.instrumentColors.put(MinecraftInstrument.HARP, Color.BLUE);
        this.instrumentColors.put(MinecraftInstrument.BASS, Color.GREEN.darker(0.6F));
        this.instrumentColors.put(MinecraftInstrument.BASS_DRUM, Color.fromRGB(150, 0, 0));
        this.instrumentColors.put(MinecraftInstrument.SNARE, Color.YELLOW);
        this.instrumentColors.put(MinecraftInstrument.HAT, Color.MAGENTA.darker());
        this.instrumentColors.put(MinecraftInstrument.GUITAR, Color.fromRGB(150, 75, 0));
        this.instrumentColors.put(MinecraftInstrument.FLUTE, Color.YELLOW.darker());
        this.instrumentColors.put(MinecraftInstrument.BELL, Color.MAGENTA);
        this.instrumentColors.put(MinecraftInstrument.CHIME, Color.CYAN.darker());
        this.instrumentColors.put(MinecraftInstrument.XYLOPHONE, Color.WHITE);
        this.instrumentColors.put(MinecraftInstrument.IRON_XYLOPHONE, Color.fromRGB(0, 200, 255));
        this.instrumentColors.put(MinecraftInstrument.COW_BELL, Color.RED.darker(0.9F));
        this.instrumentColors.put(MinecraftInstrument.DIDGERIDOO, Color.fromRGB(220, 100, 0));
        this.instrumentColors.put(MinecraftInstrument.BIT, Color.GREEN.darker(0.8F));
        this.instrumentColors.put(MinecraftInstrument.BANJO, Color.RED.darker());
        this.instrumentColors.put(MinecraftInstrument.PLING, Color.DARK_GRAY);

        this.customInstrumentColors = new IdentityHashMap<>();
        final NbsCustomInstrument[] customInstruments = SongUtil.getUsedNbsCustomInstruments(this.songPlayer.getSong()).toArray(NbsCustomInstrument[]::new);
        final float slice = 1F / (customInstruments.length + 1);
        for (int i = 0; i < customInstruments.length; i++) {
            final Color color1 = Color.fromHSB(slice * (i + 1), 0.8F, 0.8F);
            final Color color2 = Color.fromHSB(1F - slice * (i + 1), 0.8F, 0.8F);
            this.customInstrumentColors.put(customInstruments[i], new ObjectObjectImmutablePair<>(color1, color2));
        }

        this.pianoKeyPositions = new float[PIANO_KEY_COUNT];
        this.pianoKeyLastPlayed = new long[PIANO_KEY_COUNT];
        this.pianoKeyLastColors = new Color[PIANO_KEY_COUNT];
    }

    public void free() {
        if (this.robotoFont != null) {
            this.robotoFont.free();
        }
        if (this.noteBlockTexture != null) {
            this.noteBlockTexture.free();
        }
    }

    public void render(final Matrix4fStack positionMatrix) {
        final int height = ThinGL.windowInterface().getFramebufferHeight();
        this.renderedNotes = 0;
        this.updatePianoKeyPositions();

        this.drawNotes(positionMatrix);
        ThinGL.globalDrawBatch().draw();

        positionMatrix.pushMatrix();
        positionMatrix.translate(0, height - (int) (height / PIANO_HEIGHT_DIVIDER), 0);
        this.drawPiano(positionMatrix);
        positionMatrix.popMatrix();
        this.drawDebugText(positionMatrix);
        ThinGL.globalDrawBatch().draw();
    }

    private void drawNotes(final Matrix4fStack positionMatrix) {
        final int width = ThinGL.windowInterface().getFramebufferWidth();
        final int height = ThinGL.windowInterface().getFramebufferHeight() - (int) (ThinGL.windowInterface().getFramebufferHeight() / PIANO_HEIGHT_DIVIDER);
        final float whiteKeyWidth = (float) width / WHITE_PIANO_KEY_COUNT;
        final float blackKeyWidth = whiteKeyWidth * BLACK_KEY_WIDTH_RATIO;
        final float noteSize = 16 * Math.max(1, width / 960);

        final Song song = this.songPlayer.getSong();
        final int tickWindow = MathUtils.ceilInt(height / noteSize);
        final int currentTick = this.songPlayer.getTick() - 1; // SongPlayer advances the tick immediately after playing the previous one
        final int endTick = currentTick + tickWindow;
        final float ticksPerSecond = this.songPlayer.getCurrentTicksPerSecond();
        final long lastTickTime = this.songPlayer.getLastTickTime();
        final boolean paused = this.songPlayer.isPaused();
        final long timeSinceLastTick = System.nanoTime() - lastTickTime;
        final float tickProgress = !paused ? MathUtils.clamp(timeSinceLastTick / (1_000_000_000F / ticksPerSecond), 0F, 1F) : 0F;

        ThinGL.renderer2D().beginGlobalBuffering();
        ThinGL.rendererText().beginGlobalBuffering();
        for (int tick = endTick; tick >= currentTick - 1; tick--) {
            final float y = height - (tick - currentTick + 1 - tickProgress) * noteSize;
            for (Note note : song.getNotes().getOrEmpty(tick)) {
                final int nbsKey = note.getNbsKey();
                if (nbsKey < 0 || nbsKey >= this.pianoKeyPositions.length) {
                    continue;
                }
                float x = this.pianoKeyPositions[nbsKey] + (whiteKeyWidth * note.getFractionalKeyPart());
                if (!this.isBlackKey(nbsKey)) {
                    x += whiteKeyWidth / 2F;
                } else {
                    x += blackKeyWidth / 2F;
                }
                x -= noteSize / 2F;

                final float alpha = MathUtils.clamp(note.getVolume(), 0.25F, 1F);
                if (note.getInstrument() instanceof MinecraftInstrument instrument) {
                    final Color color = this.instrumentColors.getOrDefault(instrument, Color.WHITE);
                    this.pianoKeyLastColors[nbsKey] = color;
                    ThinGL.renderer2D().colorizedTexture(positionMatrix, this.noteBlockTexture, x, y, noteSize, noteSize, color.withAlphaF(alpha));
                } else if (note.getInstrument() instanceof NbsCustomInstrument instrument) {
                    final Pair<Color, Color> colors = this.customInstrumentColors.get(instrument);
                    final Color color1 = colors.left();
                    final Color color2 = colors.right();
                    this.pianoKeyLastColors[nbsKey] = Color.interpolate(0.5F, color1, color2);
                    ExtendedThinGL.renderer2D().gradientColorizedTexture(positionMatrix, this.noteBlockTexture, x, y, noteSize, noteSize, color1.withAlphaF(alpha), color2.withAlphaF(alpha));
                } else {
                    ThinGL.renderer2D().filledRectangle(positionMatrix, x, y, x + noteSize, y + noteSize, Color.WHITE.withAlphaF(alpha));
                }
                if (tick == currentTick) {
                    final long currentTime = System.nanoTime();
                    if (currentTime - this.pianoKeyLastPlayed[nbsKey] < currentTime + KEY_ANIMATION_DURATION / 2) {
                        this.pianoKeyLastPlayed[nbsKey] = currentTime - KEY_ANIMATION_DURATION / 2;
                    } else {
                        this.pianoKeyLastPlayed[nbsKey] = System.nanoTime();
                    }
                }
                this.renderedNotes++;
            }
            if (song.getTempoEvents().get(tick) != 0) {
                final float bottomY = y + noteSize;
                final float tps = song.getTempoEvents().get(tick);
                final ShapedTextRun tempoText = TextRun.fromString(this.robotoFont, "Tempo: " + String.format("%.2f", tps) + " t/s").shape();

                ThinGL.rendererText().pushGlobalScale(ThinGL.windowInterface().getFramebufferWidth() / 2000F);
                ThinGL.rendererText().textRun(positionMatrix, tempoText, 10, bottomY, RendererText.VerticalOrigin.LOGICAL_BOTTOM, RendererText.HorizontalOrigin.VISUAL_LEFT);
                ThinGL.rendererText().popGlobalScale();

                ThinGL.renderer2D().filledRectangle(positionMatrix, 0, bottomY, width, bottomY + 1, Color.WHITE.withAlpha(100));
            }
        }
        ThinGL.renderer2D().endBuffering();
        ThinGL.rendererText().endBuffering();
    }

    private void drawPiano(final Matrix4fStack positionMatrix) {
        final int width = ThinGL.windowInterface().getFramebufferWidth();
        final int height = (int) (ThinGL.windowInterface().getFramebufferHeight() / PIANO_HEIGHT_DIVIDER);
        final float whiteKeyWidth = (float) width / WHITE_PIANO_KEY_COUNT;
        final float blackKeyWidth = whiteKeyWidth * BLACK_KEY_WIDTH_RATIO;

        ThinGL.renderer2D().beginGlobalBuffering();
        ThinGL.rendererText().beginGlobalBuffering();

        final float whiteKeyLineOffset = height / WHITE_KEY_LINE_OFFSET_RATIO;
        final float blackKeyLineOffset = height / BLACK_KEY_LINE_OFFSET_RATIO;
        for (int nbsKey = 0; nbsKey < this.pianoKeyPositions.length; nbsKey++) {
            final float x = this.pianoKeyPositions[nbsKey];
            final float progress = this.pianoKeyLastColors[nbsKey] != null ? MathUtils.clamp((System.nanoTime() - this.pianoKeyLastPlayed[nbsKey]) / (float) KEY_ANIMATION_DURATION, 0F, 1F) : 1F;
            final float colorProgress = progress < 0.5F ? 0 : (progress - 0.5F) * 2;
            final float pressOffset = height / KEY_PRESS_DEPTH_RATIO - height / KEY_PRESS_DEPTH_RATIO * (progress < 0.5F ? 1F - progress : progress);
            if (!this.isBlackKey(nbsKey)) {
                final ShapedTextRun noteName = TextRun.fromString(this.robotoFont, this.getNoteName(nbsKey), Color.BLACK).shape();
                ThinGL.renderer2D().filledRectangle(positionMatrix, x + 1, pressOffset, x + whiteKeyWidth - 1, height, Color.WHITE);
                if (this.pianoKeyLastColors[nbsKey] != null) {
                    ThinGL.renderer2D().filledRectangle(positionMatrix, x + 1, pressOffset, x + whiteKeyWidth - 1, height, this.pianoKeyLastColors[nbsKey].withAlpha(Math.round(PRESSED_KEY_COLOR_ALPHA * (1 - colorProgress))));
                }
                ThinGL.renderer2D().filledRectangle(positionMatrix, x, height - whiteKeyLineOffset + pressOffset - KEY_LINE_HEIGHT, x + whiteKeyWidth, height - whiteKeyLineOffset + pressOffset, Color.GRAY);
                ThinGL.renderer2D().filledRectangle(positionMatrix, x, pressOffset, x + 1, height, Color.BLACK);
                ThinGL.renderer2D().filledRectangle(positionMatrix, x + whiteKeyWidth - 1, pressOffset, x + whiteKeyWidth, height, Color.BLACK);

                ThinGL.rendererText().pushGlobalScale(ThinGL.windowInterface().getFramebufferWidth() / 2745F); // 0,7
                ThinGL.rendererText().textRun(positionMatrix, noteName, x + whiteKeyWidth / 2, height - whiteKeyLineOffset + pressOffset - KEY_LINE_HEIGHT - this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale(), RendererText.VerticalOrigin.VISUAL_TOP, RendererText.HorizontalOrigin.VISUAL_CENTER);
                ThinGL.rendererText().popGlobalScale();
            } else {
                positionMatrix.pushMatrix();
                positionMatrix.translate(0, 0, 1);
                final ShapedTextRun noteName = TextRun.fromString(this.robotoFont, this.getNoteName(nbsKey), Color.WHITE).shape();

                ThinGL.renderer2D().filledRectangle(positionMatrix, x, pressOffset - height / KEY_PRESS_DEPTH_RATIO / 2, x + blackKeyWidth, height * BLACK_KEY_HEIGHT_RATIO, Color.BLACK);
                if (this.pianoKeyLastColors[nbsKey] != null) {
                    ThinGL.renderer2D().filledRectangle(positionMatrix, x, pressOffset - height / KEY_PRESS_DEPTH_RATIO / 2, x + blackKeyWidth, height * BLACK_KEY_HEIGHT_RATIO, this.pianoKeyLastColors[nbsKey].withAlpha(Math.round(PRESSED_KEY_COLOR_ALPHA * (1 - colorProgress))));
                }
                ThinGL.renderer2D().filledRectangle(positionMatrix, x, height * BLACK_KEY_HEIGHT_RATIO - blackKeyLineOffset + pressOffset - KEY_LINE_HEIGHT, x + blackKeyWidth, height * BLACK_KEY_HEIGHT_RATIO - blackKeyLineOffset + pressOffset, Color.GRAY);

                ThinGL.rendererText().pushGlobalScale(ThinGL.windowInterface().getFramebufferWidth() / 4000F); // 0,48
                ThinGL.rendererText().textRun(positionMatrix, noteName, x + blackKeyWidth / 2, height * BLACK_KEY_HEIGHT_RATIO - blackKeyLineOffset + pressOffset - KEY_LINE_HEIGHT - this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale(), RendererText.VerticalOrigin.VISUAL_TOP, RendererText.HorizontalOrigin.VISUAL_CENTER);
                ThinGL.rendererText().popGlobalScale();

                positionMatrix.popMatrix();
            }
        }

        ThinGL.renderer2D().endBuffering();
        ThinGL.rendererText().endBuffering();
    }

    private void drawDebugText(final Matrix4fStack positionMatrix) {
        ThinGL.rendererText().beginGlobalBuffering();
        ThinGL.rendererText().pushGlobalScale(ThinGL.windowInterface().getFramebufferWidth() / 1920F);

        float textY = 5;
        ThinGL.rendererText().textRun(positionMatrix, TextRun.fromString(this.robotoFont, "FPS: " + ThinGL.get().getFPS()), 5, textY);
        textY += this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale();

        final int seconds = (int) Math.ceil(this.songPlayer.getMillisecondPosition() / 1000F);
        final String currentPosition = String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
        ThinGL.rendererText().textRun(positionMatrix, TextRun.fromString(this.robotoFont, "Position: " + currentPosition + " / " + this.songPlayer.getSong().getHumanReadableLength()), 5, textY);
        textY += this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale();

        ThinGL.rendererText().textRun(positionMatrix, TextRun.fromString(this.robotoFont, "Tempo: " + String.format("%.2f", this.songPlayer.getCurrentTicksPerSecond()) + " t/s"), 5, textY);
        textY += this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale();

        if (this.songPlayer.isRunning()) {
            for (String statusLine : this.songPlayer.getStatusLines()) {
                ThinGL.rendererText().textRun(positionMatrix, TextRun.fromString(this.robotoFont, statusLine), 5, textY);
                textY += this.robotoFont.getHeight() * ThinGL.rendererText().getGlobalScale();
            }
        }

        ThinGL.rendererText().textRun(positionMatrix, TextRun.fromString(this.robotoFont, "Rendered Notes: " + this.renderedNotes), 5, textY);

        ThinGL.rendererText().popGlobalScale();
        ThinGL.rendererText().endBuffering();
    }

    private boolean isBlackKey(final int nbsKey) {
        final int noteInOctave = (nbsKey + NbsDefinitions.NBS_LOWEST_MIDI_KEY) % 12;
        return noteInOctave == 1 || noteInOctave == 3 || noteInOctave == 6 || noteInOctave == 8 || noteInOctave == 10;
    }

    private String getNoteName(final int nbsKey) {
        final int noteInOctave = nbsKey % 12;
        String noteName = switch (noteInOctave) {
            case 0 -> "A";
            case 1 -> "A#";
            case 2 -> "B";
            case 3 -> "C";
            case 4 -> "C#";
            case 5 -> "D";
            case 6 -> "D#";
            case 7 -> "E";
            case 8 -> "F";
            case 9 -> "F#";
            case 10 -> "G";
            case 11 -> "G#";
            default -> throw new IllegalStateException("Unexpected value: " + noteInOctave);
        };
        noteName += (nbsKey + NbsDefinitions.NBS_LOWEST_MIDI_KEY) / 12 - 1;
        return noteName;
    }

    private void updatePianoKeyPositions() {
        Arrays.fill(this.pianoKeyPositions, 0F);
        final int width = ThinGL.windowInterface().getFramebufferWidth();
        final float whiteKeyWidth = (float) width / WHITE_PIANO_KEY_COUNT;
        final float blackKeyWidth = whiteKeyWidth * BLACK_KEY_WIDTH_RATIO;
        float whiteX = 0;
        for (int nbsKey = 0; nbsKey < PIANO_KEY_COUNT; nbsKey++) {
            if (!this.isBlackKey(nbsKey)) {
                this.pianoKeyPositions[nbsKey] = whiteX;
                whiteX += whiteKeyWidth;
            } else {
                this.pianoKeyPositions[nbsKey] = whiteX - (blackKeyWidth / 2F);
            }
        }
    }

}
