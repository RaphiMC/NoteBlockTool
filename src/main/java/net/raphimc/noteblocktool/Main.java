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
package net.raphimc.noteblocktool;

import com.formdev.flatlaf.FlatDarkLaf;
import net.raphimc.noteblocktool.frames.ListFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        FlatDarkLaf.setup();
        UIManager.getLookAndFeelDefaults().put("TextComponent.arc", 5);
        UIManager.getLookAndFeelDefaults().put("Button.arc", 5);
        ToolTipManager.sharedInstance().setInitialDelay(100);
        ToolTipManager.sharedInstance().setDismissDelay(10_000);

        new ListFrame();
    }

}
