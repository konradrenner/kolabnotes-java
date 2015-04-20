/**
 * 
 */
package org.kore.kolab.notes.v3;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.kore.kolab.notes.KolabNotesParser;
import org.kore.kolab.notes.Note;
import org.w3c.dom.Document;

/**
 * Parser for Kolab Notes V3 Format
 * 
 * @author Konrad Renner
 * 
 */
public class KolabNotesParserV3
        implements KolabNotesParser, Serializable {

    /* (non-Javadoc)
     * @see org.kore.kolabnotes.KolabNotesParser#parseNote(java.io.InputStream)
     */
    @Override
    public Note parseNote(InputStream stream) {
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
     * @see org.kore.kolabnotes.KolabNotesParser#writeNote(org.kore.kolabnotes.Note, java.io.OutputStream)
     */
    @Override
    public void writeNote(Note note, OutputStream stream) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document document = KolabNotesXMLBuilder.createInstance(docBuilder)
                    .withIdentification(note.getIdentification())
                    .withAuditInformation(note.getAuditInformation())
                    .withClassification(note.getClassification())
                    .withSummary(note.getSummary())
                    .withDescription(note.getDescription())
                    .withCategories(note.getCategories())
                    .build();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(stream);

            transformer.transform(source, result);
        } catch (Exception e) {
            throw new KolabParseException(e);
        }
    }

}
