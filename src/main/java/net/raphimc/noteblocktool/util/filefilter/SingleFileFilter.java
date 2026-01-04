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
package net.raphimc.noteblocktool.util.filefilter;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class SingleFileFilter extends FileFilter {

    private final String extension;

    public SingleFileFilter(final String extension) {
        this.extension = extension.toLowerCase();
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) return true;
        if (!f.isFile()) return false;
        return f.getName().toLowerCase().endsWith("." + this.extension);
    }

    @Override
    public String getDescription() {
        return this.extension + " File";
    }

}
