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
package net.raphimc.noteblocktool.elements;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class FastScrollPane extends JScrollPane {

    private final Border defaultBorder = this.getBorder();

    public FastScrollPane() {
    }

    public FastScrollPane(final Component view) {
        super(view);
    }

    {
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    public FastScrollPane setDefaultBorder() {
        this.setBorder(this.defaultBorder);
        return this;
    }

    public Border getDefaultBorder() {
        return this.defaultBorder;
    }

    @Override
    public JScrollBar createVerticalScrollBar() {
        JScrollBar scrollBar = super.createVerticalScrollBar();
        scrollBar.setUnitIncrement(16);
        scrollBar.setBlockIncrement(16);
        return scrollBar;
    }

    @Override
    public JScrollBar createHorizontalScrollBar() {
        JScrollBar scrollBar = super.createHorizontalScrollBar();
        scrollBar.setUnitIncrement(16);
        scrollBar.setBlockIncrement(16);
        return scrollBar;
    }

}
