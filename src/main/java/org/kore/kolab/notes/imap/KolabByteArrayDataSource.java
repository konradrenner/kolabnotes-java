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
package org.kore.kolab.notes.imap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import korex.mail.util.ByteArrayDataSource;
import org.kore.kolab.notes.Attachment;

/**
 *
 * @author Konrad Renner
 */
public class KolabByteArrayDataSource extends ByteArrayDataSource {

    private Attachment attachment;

    public KolabByteArrayDataSource(Attachment att) {
        super(att.getData(), att.getMimeType());
        attachment = att;
    }

    public KolabByteArrayDataSource(InputStream is, String type) throws IOException {
        super(is, type);
    }

    public KolabByteArrayDataSource(byte[] data, String type) {
        super(data, type);
    }

    public KolabByteArrayDataSource(String data, String type) throws IOException {
        super(data, type);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new ByteArrayOutputStream(); //To change body of generated methods, choose Tools | Templates.
    }

}
