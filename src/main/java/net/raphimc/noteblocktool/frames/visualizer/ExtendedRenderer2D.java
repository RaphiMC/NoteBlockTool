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
package net.raphimc.noteblocktool.frames.visualizer;

import net.lenni0451.commons.color.Color;
import net.raphimc.thingl.gl.renderer.impl.Renderer2D;
import net.raphimc.thingl.gl.resource.image.texture.impl.Texture2D;
import net.raphimc.thingl.rendering.bufferbuilder.impl.VertexBufferBuilder;
import org.joml.Matrix4f;

public class ExtendedRenderer2D extends Renderer2D {

    public void gradientColorizedTexture(final Matrix4f positionMatrix, final Texture2D texture, final float x, final float y, final float width, final float height, final Color color1, final Color color2) {
        final VertexBufferBuilder vertexBufferBuilder = this.targetMultiDrawBatchDataHolder.getVertexBufferBuilder(this.colorizedTextureQuad.apply(texture.getGlId()));
        vertexBufferBuilder.writeVector3f(positionMatrix, x, y + height, 0F).writeColor(color2).writeTextureCoord(0F, 1F).endVertex();
        vertexBufferBuilder.writeVector3f(positionMatrix, x + width, y + height, 0F).writeColor(color2).writeTextureCoord(1F, 1F).endVertex();
        vertexBufferBuilder.writeVector3f(positionMatrix, x + width, y, 0F).writeColor(color1).writeTextureCoord(1F, 0F).endVertex();
        vertexBufferBuilder.writeVector3f(positionMatrix, x, y, 0F).writeColor(color1).writeTextureCoord(0F, 0F).endVertex();
        this.drawIfNotBuffering();
    }

}
