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
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.kore.kolab.notes.Attachment;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Color;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;

/**
 * @author Konrad Renner
 * 
 */
public final class KolabNotesXMLBuilder {

    private static final Pattern PATTERN_HTML_START = Pattern.compile("(<|&lt;)html(>|&gt;)");
    private static final Pattern PATTERN_BODY_START = Pattern.compile("(<|&lt;)body(>|&gt;)");
    private static final Pattern PATTERN_BODY_END = Pattern.compile("(</|&lt;/)body(>|&gt;)");

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

    public KolabNotesXMLBuilder withAttachments(Collection<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            builder.append("<attachment>");
            builder.append("<parameters>");
            builder.append("<fmttype>");
            builder.append(attachment.getMimeType());
            builder.append("</fmttype>");
            builder.append("<x-label>");
            builder.append(attachment.getFileName());
            builder.append("</x-label>");
            builder.append("</parameters>");
            builder.append("<uri>");
            builder.append("cid:");
            builder.append(attachment.getId());
            builder.append("</uri>");
            builder.append("</attachment>");
        }
        return this;
    }

    public KolabNotesXMLBuilder withDescription(String desc) {
        if (desc != null) {
            builder.append("<description>");

            String correct = desc;
            Matcher html = PATTERN_HTML_START.matcher(correct);
            if (html.find()) {
                Matcher start = PATTERN_BODY_START.matcher(correct);
                if (start.find()) {
                    Matcher ende = PATTERN_BODY_END.matcher(correct);
                    //must be found, otherwise the description would be not correct
                    ende.find(start.end());
                    correct = replaceHtmlCharacters(correct, start.end(), ende.end() + 1);
                }
            } else {
                correct = replaceHtmlCharacters(correct, 0, correct.length());
            }

            correct = correct.replaceAll(">", "&gt;").replaceAll("<", "&lt;").replaceAll("&nbsp;", " ");

            builder.append(correct);
            builder.append("</description>");
        } else {
            builder.append("<description/>");
        }
        return this;
    }

    private String replaceHtmlCharacters(String correct, int start, int ende) {
        //there must be an html end tag after body, so no check if length would be greater than length of correct
        String newBody = correct.substring(start, ende);
        newBody = newBody.replaceAll("&nbsp;", " ").replaceAll("&(?!amp;)", "&amp;");
        StringBuilder sb = new StringBuilder(correct);
        correct = sb.replace(start, ende, newBody).toString();
        return correct;
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
