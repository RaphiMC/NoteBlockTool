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
import java.awt.*;

public class VerticalFileChooser extends JFileChooser {

    public VerticalFileChooser() {
        this.changeListToVertical(this);
    }

    private void changeListToVertical(final JComponent component) {
        for (Component c : component.getComponents()) {
            if (c instanceof JList<?>) {
                JList<?> list = (JList<?>) c;
                list.setLayoutOrientation(JList.VERTICAL);
            }
            if (c instanceof JComponent) {
                this.changeListToVertical((JComponent) c);
            }
        }
    }

}
