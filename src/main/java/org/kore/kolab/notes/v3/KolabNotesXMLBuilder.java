/**
 * 
 */
package org.kore.kolab.notes.v3;

import java.text.SimpleDateFormat;
import java.util.Set;
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
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat time = new SimpleDateFormat("hh:mm:ss");

        String creation = date.format(id.getCreationDate()) + "T" + time.format(id.getCreationDate()) + "Z";
        String modification = date.format(id.getLastModificationDate()) + "T"
                + time.format(id.getLastModificationDate())
                + "Z";

        Element element = doc.createElement("creation-date");
        element.appendChild(doc.createTextNode(creation));
        rootElement.appendChild(element);

        element = doc.createElement("last-modification-date");
        element.appendChild(doc.createTextNode(modification));
        rootElement.appendChild(element);
        return this;
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
