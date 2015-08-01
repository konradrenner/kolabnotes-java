/**
 * 
 */
package org.kore.kolab.notes.v3;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import korex.mail.internet.AsciiOutputStream;
import org.kore.kolab.notes.KolabParser;
import org.kore.kolab.notes.Note;

/**
 * Parser for Kolab Notes V3 Format
 * 
 * @author Konrad Renner
 * 
 */
public class KolabNotesParserV3
        implements KolabParser, Serializable {

    /* (non-Javadoc)
     * @see org.kore.kolabnotes.KolabParser#parse(java.io.InputStream)
     */
    @Override
    public Note parse(InputStream stream) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            KolabNotesHandler handler = new KolabNotesHandler();
            saxParser.parse(stream, handler);

            return handler.getNote();
        } catch (Exception e) {
            throw new KolabParseException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.kore.kolabnotes.KolabParser#write(org.kore.kolabnotes.Note, java.io.OutputStream)
     */
    @Override
    public void write(Object object, OutputStream stream) {
        try {
            Note note = (Note) object;
            //order of the builder methods is important for validation against schema
            String xml = new KolabNotesXMLBuilder()
                    .withIdentification(note.getIdentification())
                    .withAuditInformation(note.getAuditInformation())
                    .withClassification(note.getClassification())
                    .withSummary(note.getSummary())
                    .withDescription(note.getDescription())
                    .withColor(note.getColor())
                    .build();

            if (stream instanceof AsciiOutputStream) {
                ((AsciiOutputStream) stream).disableBreakOnNonAscii();
            }

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);

            outputStreamWriter.append(xml);
            outputStreamWriter.flush();
        } catch (Exception e) {
            throw new KolabParseException(e);
        }
    }

}
