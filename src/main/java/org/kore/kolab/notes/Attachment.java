/*
 * Copyright (C) 2016 Konrad Renner
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

/**
 *
 * @author Konrad Renner
 */
public class Attachment {

    private final String id;
    private final String mimeType;
    private final String fileName;
    private byte[] data;

    public Attachment(String id, String fileName, String mimeType) {
        this.id = id;
        this.data = new byte[0];
        this.fileName = fileName;
        this.mimeType = mimeType;
    }

    public Attachment(String fileName, String mimeType) {
        this(fileName, fileName, mimeType);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] bytes) {
        this.data = bytes;
    }

    public String getId() {
        return id;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.id != null ? this.id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Attachment other = (Attachment) obj;
        if ((this.id == null) ? (other.id != null) : !this.id.equals(other.id)) {
            return false;
        }
        return true;
    }

}
