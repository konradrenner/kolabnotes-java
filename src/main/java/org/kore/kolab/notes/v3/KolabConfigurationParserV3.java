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
import org.kore.kolab.notes.KolabParser;
import org.kore.kolab.notes.imap.RemoteTags;
import org.w3c.dom.Document;

/**
 * Parser for Kolab Notes V3 Format
 * 
 * @author Konrad Renner
 * 
 */
public class KolabConfigurationParserV3
        implements KolabParser, Serializable {

    /* (non-Javadoc)
     * @see org.kore.kolabnotes.KolabParser#parse(java.io.InputStream)
     */
    @Override
    public RemoteTags.TagDetails parse(InputStream stream) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            KolabConfigurationHandler handler = new KolabConfigurationHandler();
            saxParser.parse(stream, handler);

            return handler.getTag();
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
            RemoteTags.TagDetails details = (RemoteTags.TagDetails) object;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //order of the builder methods is important for validation against schema
            Document document = KolabConfigurationXMLBuilder.createInstance(docBuilder)
                    .withIdentification(details.getIdentification())
                    .withAuditInformation(details.getAuditInformation())
                    .withType()
                    .withName(details.getTag().getName())
                    .withRelationType()
                    .withColor(details.getTag().getColor())
                    .withPriority(details.getTag().getPriority())
                    .withMembers(details.getMembers())
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
