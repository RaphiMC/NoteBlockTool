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
package net.raphimc.noteblocktool.util;

import java.util.Arrays;

public class CircularBuffer {

    private final byte[] buffer;
    private int head;
    private int tail;
    private int currentSize;

    public CircularBuffer(final int capacity) {
        this.buffer = new byte[capacity];
    }

    public void add(final byte value) {
        if (this.isFull()) {
            throw new IllegalStateException("Buffer is full");
        }
        this.buffer[this.tail] = value;
        this.tail = (this.tail + 1) % this.buffer.length;
        this.currentSize++;
    }

    public void addAll(final byte[] values) {
        this.addAll(values, values.length);
    }

    public void addAll(final byte[] values, final int valuesLength) {
        if (valuesLength > (this.buffer.length - currentSize)) {
            throw new IllegalStateException("Not enough space in the buffer");
        }

        final int firstPartLength = Math.min(valuesLength, this.buffer.length - this.tail);
        System.arraycopy(values, 0, this.buffer, this.tail, firstPartLength);

        if (firstPartLength < valuesLength) {
            final int secondPartLength = valuesLength - firstPartLength;
            System.arraycopy(values, firstPartLength, this.buffer, 0, secondPartLength);
            this.tail = secondPartLength;
        } else {
            this.tail = (this.tail + valuesLength) % this.buffer.length;
        }

        this.currentSize += valuesLength;
    }

    public byte take() {
        if (this.isEmpty()) {
            throw new IllegalStateException("Buffer is empty");
        }
        return this.takeSafe();
    }

    public byte takeSafe() {
        if (this.isEmpty()) {
            return 0;
        }
        final byte value = this.buffer[this.head];
        this.head = (head + 1) % this.buffer.length;
        this.currentSize--;
        return value;
    }

    public byte[] takeAllSafe(final int size) {
        final byte[] values = new byte[size];
        this.takeAllSafe(values);
        return values;
    }

    public void takeAllSafe(final byte[] values) {
        /*if (size > currentSize) {
            throw new IllegalArgumentException("Requested size exceeds the number of elements in the buffer");
        }*/

        final int elementsToRemove = Math.min(values.length, this.currentSize);

        final int firstPartLength = Math.min(elementsToRemove, this.buffer.length - this.head);
        System.arraycopy(this.buffer, this.head, values, 0, firstPartLength);

        if (firstPartLength < elementsToRemove) {
            final int secondPartLength = elementsToRemove - firstPartLength;
            System.arraycopy(this.buffer, 0, values, firstPartLength, secondPartLength);
        }

        this.head = (this.head + elementsToRemove) % this.buffer.length;
        this.currentSize -= elementsToRemove;

        Arrays.fill(values, elementsToRemove, values.length, (byte) 0);
    }

    public boolean hasSpaceFor(final int amount) {
        return this.currentSize + amount <= this.buffer.length;
    }

    public boolean isEmpty() {
        return this.currentSize == 0;
    }

    public boolean isFull() {
        return this.currentSize == this.buffer.length;
    }

    public int getCurrentSize() {
        return this.currentSize;
    }

    public int getCapacity() {
        return this.buffer.length;
    }

}
