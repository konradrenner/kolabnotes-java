/**
 * 
 */
package org.kore.kolab.notes.v3;

import java.util.Calendar;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Konrad Renner
 * 
 */
public final class KolabConfigurationXMLBuilder {

    private final Document doc;
    private final Element rootElement;

    public KolabConfigurationXMLBuilder(Document doc, Element rootElement) {
        this.doc = doc;
        this.rootElement = rootElement;
    }

    public static final KolabConfigurationXMLBuilder createInstance(DocumentBuilder builder) {
        Document doc = builder.newDocument();
        Element root = doc.createElement("configuration");
        root.setAttribute("xmlns", "http://kolab.org");
        root.setAttribute("version", "3.0");
        doc.appendChild(root);

        return new KolabConfigurationXMLBuilder(doc, root);
    }


    public KolabConfigurationXMLBuilder withIdentification(Identification id) {
        Element element = doc.createElement("uid");
        element.appendChild(doc.createTextNode(id.getUid()));
        rootElement.appendChild(element);

        element = doc.createElement("prodid");
        element.appendChild(doc.createTextNode(id.getProductId()));
        rootElement.appendChild(element);
        return this;
    }

    public KolabConfigurationXMLBuilder withAuditInformation(AuditInformation id) {
        String creation = createTimestampString(id.getCreationDate());
        Element element = doc.createElement("creation-date");
        element.appendChild(doc.createTextNode(creation));
        rootElement.appendChild(element);

        String modification = createTimestampString(id.getLastModificationDate());
        element = doc.createElement("last-modification-date");
        element.appendChild(doc.createTextNode(modification));
        rootElement.appendChild(element);
        return this;
    }

    String createTimestampString(Date date) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);

        StringBuilder sb = new StringBuilder(String.format("%1$04d", calendar.get(YEAR)));
        sb.append('-');
        sb.append(String.format("%1$02d", (calendar.get(MONTH) + 1)));
        sb.append('-');
        sb.append(String.format("%1$02d", (calendar.get(DAY_OF_MONTH))));
        sb.append('T');
        sb.append(String.format("%1$02d", (calendar.get(HOUR_OF_DAY))));
        sb.append(':');
        sb.append(String.format("%1$02d", (calendar.get(MINUTE))));
        sb.append(':');
        sb.append(String.format("%1$02d", (calendar.get(SECOND))));
        sb.append('Z');

        return sb.toString();
    }

    public KolabConfigurationXMLBuilder withType() {
        Element element = doc.createElement("type");
        element.appendChild(doc.createTextNode("relation"));
        rootElement.appendChild(element);

        return this;
    }

    public KolabConfigurationXMLBuilder withName(String name) {
        Element element = doc.createElement("name");
        if (name != null) {
            element.appendChild(doc.createTextNode(name));
        }
        rootElement.appendChild(element);
        return this;
    }

    public KolabConfigurationXMLBuilder withRelationType() {
        Element element = doc.createElement("relationType");
        element.appendChild(doc.createTextNode("tag"));
        rootElement.appendChild(element);
        return this;
    }

    public KolabConfigurationXMLBuilder withPriority(int priority) {
        Element element = doc.createElement("priority");
        element.appendChild(doc.createTextNode(Integer.toString(priority)));
        rootElement.appendChild(element);
        return this;
    }

    public KolabConfigurationXMLBuilder withMembers(Set<String> members) {
        if (!members.isEmpty()) {
            for (String member : members) {
                Element element = doc.createElement("member");
                element.appendChild(doc.createTextNode("urn:uuid:" + member));
                rootElement.appendChild(element);
            }
        }
        return this;
    }

    public Document build() {
        return doc;
    }
}
