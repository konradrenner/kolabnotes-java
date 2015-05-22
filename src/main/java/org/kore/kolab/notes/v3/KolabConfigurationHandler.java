package org.kore.kolab.notes.v3;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.imap.RemoteTags;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class KolabConfigurationHandler
		extends DefaultHandler {

    private final TagDetailBuilder builder;
    private String currentValue = "";
    private StringBuilder completeValue;

    public KolabConfigurationHandler() {
        this.builder = new TagDetailBuilder();
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

    public RemoteTags.TagDetails getTag() {
        return builder.build();
    }

    class TagDetailBuilder {

        private String uid;
        private String productId;
        private Timestamp creationDate;
        private Timestamp lastModificationDate;
        private final Set<String> members = new LinkedHashSet<String>();
        private String name;
        private String type;
        private String relationType;
        private int priority;

        void setValue(String name, String value) throws ParseException {
            if ("uid".equals(name)) {
                uid = value;
            } else if ("prodid".equals(name)) {
                productId = value;
            } else if ("creation-date".equals(name)) {
                creationDate = convertTimestamp(value);
            } else if ("last-modification-date".equals(name)) {
                lastModificationDate = convertTimestamp(value);
            } else if ("priority".equals(name)) {
                priority = value == null || value.trim().length() == 0 ? 0 : Integer.valueOf(value);
            } else if ("type".equals(name)) {
                type = value;
            } else if ("relationType".equals(name)) {
                relationType = value;
            } else if ("name".equals(name)) {
                this.name = value;
            } else if ("member".equals(name)) {
                //remove urn:uuid: at start
                if (value.startsWith("urn:uuid:")) {
                    members.add(value.substring(9));
                } else {
                    members.add(value);
                }
            }
        }

        RemoteTags.TagDetails build() {
            Identification id = new Identification(uid, productId);
            AuditInformation auditInformation = new AuditInformation(creationDate, lastModificationDate);
            Tag tag = new Tag(name);
            tag.setPriority(priority);
            RemoteTags.TagDetails tagdetail = new RemoteTags.TagDetails(id, auditInformation, tag, members);

            if ("tag".equalsIgnoreCase(relationType) && "relation".equalsIgnoreCase(type)) {
                return tagdetail;
            }
            return null;
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
