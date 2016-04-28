package org.kore.kolab.notes.v3;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;
import org.kore.kolab.notes.Attachment;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Color;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class KolabNotesHandler
		extends DefaultHandler {

    private final NoteBuilder builder;
    private String currentValue = "";
    private StringBuilder completeValue;

    public KolabNotesHandler() {
        this.builder = new NoteBuilder();
        this.completeValue = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        completeValue = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        completeValue.append(new String(ch, start, length));
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            if (qName.contains("attachment")) {
                builder.addAttachment();
            } else {
                builder.setValue(qName, completeValue.toString());
            }
        } catch (ParseException e) {
            throw new KolabParseException(e);
        }
    }

    public Note getNote() {
        return builder.build();
    }

    class NoteBuilder {

        private String uid;
        private String productId;
        private Timestamp creationDate;
        private Timestamp lastModificationDate;
        private Note.Classification classification;
        private String summary;
        private String description;
        private Color color;
        private Set<Attachment> attachments = new LinkedHashSet<Attachment>();

        //attachment fields
        private String xlabel;
        private String fmttype;
        private String uri;
        
        void addAttachment() {
            attachments.add(new Attachment(uri, xlabel, fmttype));
        }

        void setValue(String name, String value) throws ParseException {
            if ("uid".contains(name)) {
                uid = value;
            } else if ("prodid".contains(name)) {
                productId = value;
            } else if ("creation-date".contains(name)) {
                creationDate = convertTimestamp(value);
            } else if ("last-modification-date".contains(name)) {
                lastModificationDate = convertTimestamp(value);
            } else if ("classification".contains(name)) {
                classification = Note.Classification.valueOf(value.toUpperCase());
            } else if ("summary".contains(name)) {
                summary = value;
            } else if ("description".contains(name)) {
                description = value;
            } else if ("color".contains(name)) {
                color = Colors.getColor(value);
            } else if ("fmttype".contains(name)) {
                fmttype = value.trim();
            } else if ("uri".contains(name)) {
                uri = value.substring(value.indexOf(":") + 1);
            } else if ("x-label".contains(name)) {
                xlabel = value.trim();
            }
        }

        Note build() {
            Identification id = new Identification(uid, productId);
            AuditInformation auditInformation = new AuditInformation(creationDate, lastModificationDate);
            Note note = new Note(id, auditInformation, classification, summary);
            note.setDescription(description);
            note.setSummary(summary);
            note.setColor(color);
            note.addAttachments(attachments.toArray(new Attachment[attachments.size()]));

            return note;
        }

        Timestamp convertTimestamp(String value) throws ParseException {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(Calendar.YEAR, Integer.valueOf(value.substring(0, 4)));
            calendar.set(Calendar.MONTH, Integer.valueOf(value.substring(5, 7)) - 1);
            calendar.set(Calendar.DAY_OF_MONTH, Integer.valueOf(value.substring(8, 10)));
            calendar.set(Calendar.HOUR_OF_DAY, Integer.valueOf(value.substring(11, 13)));
            calendar.set(Calendar.MINUTE, Integer.valueOf(value.substring(14, 16)));
            if (value.length() < 19) {
                calendar.set(Calendar.SECOND, Integer.valueOf(value.substring(17)));
            } else {
                calendar.set(Calendar.SECOND, Integer.valueOf(value.substring(17, 19)));
            }

            return new Timestamp(calendar.getTimeInMillis());
        }
    }
}
