/*
 * Copyright (C) 2015 Konrad Renner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kore.kolab.notes;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * @author Konrad Renner
 * 
 */
public interface KolabParser extends Serializable {

    /**
     * Parses a note from an InputStream, this method will not close the
     * InputStream!
     *
     * @param stream
     * @return Note
     */
    Object parse(InputStream stream);

    /**
     * Writes a note to an OnputStream, this method will not close the
     * OnputStream!
     *
     * @param stream
     * @param toWrite
     */
    void write(Object toWrite, OutputStream stream);

}
