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
import org.kore.kolab.notes.Note;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author Konrad Renner
 * 
 */
public final class KolabNotesXMLBuilder {

    private final Document doc;
    private final Element rootElement;

    public KolabNotesXMLBuilder(Document doc, Element rootElement) {
        this.doc = doc;
        this.rootElement = rootElement;
    }

    public static final KolabNotesXMLBuilder createInstance(DocumentBuilder builder) {
        Document doc = builder.newDocument();
        Element root = doc.createElement("note");
        root.setAttribute("xmlns", "http://kolab.org");
        root.setAttribute("version", "3.0");
        doc.appendChild(root);

        return new KolabNotesXMLBuilder(doc, root);
    }


    public KolabNotesXMLBuilder withIdentification(Note.Identification id) {
        Element element = doc.createElement("uid");
        element.appendChild(doc.createTextNode(id.getUid()));
        rootElement.appendChild(element);

        element = doc.createElement("prodid");
        element.appendChild(doc.createTextNode(id.getProductId()));
        rootElement.appendChild(element);
        return this;
    }

    public KolabNotesXMLBuilder withAuditInformation(Note.AuditInformation id) {
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

    public KolabNotesXMLBuilder withClassification(Note.Classification classification) {
        Element element = doc.createElement("classification");
        element.appendChild(doc.createTextNode(classification.name()));
        rootElement.appendChild(element);

        return this;
    }

    public KolabNotesXMLBuilder withSummary(String summray) {
        Element element = doc.createElement("summary");
        if (summray != null) {
            element.appendChild(doc.createTextNode(summray));
        }
        rootElement.appendChild(element);
        return this;
    }

    public KolabNotesXMLBuilder withDescription(String desc) {
        Element element = doc.createElement("description");
        if (desc != null) {
            element.appendChild(doc.createTextNode(desc));
        }
        rootElement.appendChild(element);
        return this;
    }

    public KolabNotesXMLBuilder withCategories(Set<String> categories) {
        if (!categories.isEmpty()) {
            Element element = doc.createElement("categories");
            StringBuilder sb = new StringBuilder();
            for (String categorie : categories) {
                sb.append(categorie);
                sb.append(',');
            }
            element.appendChild(doc.createTextNode(sb.substring(0, sb.length() - 1)));
            rootElement.appendChild(element);
        }
        return this;
    }

    public Document build() {
        return doc;
    }
}
