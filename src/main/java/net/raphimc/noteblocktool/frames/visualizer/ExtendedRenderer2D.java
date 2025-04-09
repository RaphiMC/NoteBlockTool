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

import net.lenni0451.commons.color.Color;
import net.raphimc.thingl.drawbuilder.databuilder.holder.VertexDataHolder;
import net.raphimc.thingl.renderer.impl.Renderer2D;
import org.joml.Matrix4f;

public class ExtendedRenderer2D extends Renderer2D {

    public void gradientColorizedTexture(final Matrix4f positionMatrix, final int id, final float x, final float y, final float width, final float height, final Color color1, final Color color2) {
        final VertexDataHolder vertexDataHolder = this.targetMultiDrawBatchDataHolder.getVertexDataHolder(this.colorizedTexturedQuad.apply(id));
        vertexDataHolder.position(positionMatrix, x, y + height, 0F).color(color2).texture(0F, 1F).endVertex();
        vertexDataHolder.position(positionMatrix, x + width, y + height, 0F).color(color2).texture(1F, 1F).endVertex();
        vertexDataHolder.position(positionMatrix, x + width, y, 0F).color(color1).texture(1F, 0F).endVertex();
        vertexDataHolder.position(positionMatrix, x, y, 0F).color(color1).texture(0F, 0F).endVertex();
        this.drawIfNotBuffering();
    }

}
