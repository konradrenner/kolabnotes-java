package org.kore.kolab.notes.v3;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;
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

    public void characters(char[] ch, int start, int length) throws SAXException {
        completeValue.append(new String(ch, start, length));
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        try {
            builder.setValue(qName, completeValue.toString());
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
        private Set<String> categories;
        private Note.Classification classification;
        private String summary;
        private String description;

        void setValue(String name, String value) throws ParseException {
            if ("uid".equals(name)) {
                uid = value;
            } else if ("prodid".equals(name)) {
                productId = value;
            } else if ("creation-date".equals(name)) {
                creationDate = convertTimestamp(value);
            } else if ("last-modification-date".equals(name)) {
                lastModificationDate = convertTimestamp(value);
            } else if ("classification".equals(name)) {
                classification = Note.Classification.valueOf(value.toUpperCase());
            } else if ("summary".equals(name)) {
                summary = value;
            } else if ("description".equals(name)) {
                description = value;
            } else if ("categories".equals(name)) {
                categories = new LinkedHashSet<String>();
                if (value != null && value.trim().length() > 0) {
                    String correctValue = value;
                    if (value.startsWith("CATEGORIES:")) {
                        correctValue = value.substring(11);
                    }
                    String[] all = correctValue.split(",");
                    categories.addAll(Arrays.asList(all));
                }
            }
        }

        Note build() {
            Note.Identification id = new Note.Identification(uid, productId);
            Note.AuditInformation auditInformation = new Note.AuditInformation(creationDate, lastModificationDate);
            Note note = new Note(id, auditInformation, classification, summary);
            note.setDescription(description);
            note.setSummary(summary);

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
