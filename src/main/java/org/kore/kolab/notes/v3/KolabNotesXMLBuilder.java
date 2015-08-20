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
import java.util.TimeZone;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Color;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;

/**
 * @author Konrad Renner
 * 
 */
public final class KolabNotesXMLBuilder {

    private final StringBuilder builder;

    public KolabNotesXMLBuilder() {
        builder = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><note xmlns=\"http://kolab.org\" version=\"3.0\">");
    }


    public KolabNotesXMLBuilder withIdentification(Identification id) {
        builder.append("<uid>");
        builder.append(id.getUid());
        builder.append("</uid>");

        builder.append("<prodid>");
        builder.append(id.getProductId());
        builder.append("</prodid>");
        return this;
    }

    public KolabNotesXMLBuilder withAuditInformation(AuditInformation id) {
        builder.append("<creation-date>");
        String creation = createTimestampString(id.getCreationDate());
        builder.append(creation);
        builder.append("</creation-date>");

        builder.append("<last-modification-date>");
        String modification = createTimestampString(id.getLastModificationDate());
        builder.append(modification);
        builder.append("</last-modification-date>");
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
        builder.append("<classification>");
        builder.append(classification.name());
        builder.append("</classification>");

        return this;
    }

    public KolabNotesXMLBuilder withSummary(String summray) {
        if (summray != null) {
            builder.append("<summary>");
            builder.append(summray);
            builder.append("</summary>");
        } else {
            builder.append("<summary/>");
        }
        return this;
    }

    public KolabNotesXMLBuilder withDescription(String desc) {
        if (desc != null) {
            builder.append("<description>");

            String correct = desc.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&nbsp;", " ");

            builder.append(correct);
            builder.append("</description>");
        } else {
            builder.append("<description/>");
        }
        return this;
    }

    public KolabNotesXMLBuilder withColor(Color color) {
        if (color != null) {
            builder.append("<color>");
            builder.append(color.getHexcode());
            builder.append("</color>");
        } else {
            builder.append("<color/>");
        }
        return this;
    }

    public String build() {
        return builder.append("</note>").toString();
    }
}
