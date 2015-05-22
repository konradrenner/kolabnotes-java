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
package org.kore.kolab.notes.imap;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import kore.awt.datatransfer.DataFlavor;
import kore.awt.datatransfer.UnsupportedFlavorException;
import korex.activation.ActivationDataFlavor;
import korex.activation.DataHandler;
import org.kore.kolab.notes.KolabParser;
import org.kore.kolab.notes.Note;

/**
 *
 * @author Konrad Renner
 */
public class IMAPKolabDataHandler extends DataHandler {

    private static final ActivationDataFlavor myDF = new ActivationDataFlavor(
            Note.class,
            "APPLICATION/VND.KOLAB+XML",
            "Kolab Object");

    /**
     * An OuputStream wrapper that doesn't close the underlying stream.
     */
    private static class NoCloseOutputStream extends FilterOutputStream {

        public NoCloseOutputStream(OutputStream os) {
            super(os);
        }

        @Override
        public void close() {
            // do nothing
        }
    }

    private final KolabParser parser;

    public IMAPKolabDataHandler(Object obj, String mimeType, KolabParser parser) {
        super(obj, mimeType);
        this.parser = parser;
    }

    /**
     * Return the DataFlavors for this <code>DataContentHandler</code>.
     *
     * @return The DataFlavors
     */
    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{myDF};
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (isDataFlavorSupported(flavor)) {
            return getContent();
        } else {
            return null;
        }
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return myDF.equals(flavor);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        parser.write(getContent(), os);
    }

    @Override
    public String getContentType() {
        return "APPLICATION/VND.KOLAB+XML";
    }
}
