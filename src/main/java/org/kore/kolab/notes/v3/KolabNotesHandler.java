package org.kore.kolab.notes.v3;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
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

		private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
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
				String replaced = value.replaceAll("T", " ").replaceAll("Z", "");
				creationDate = new Timestamp(format.parse(replaced).getTime());
			} else if ("last-modification-date".equals(name)) {
				String replaced = value.replaceAll("T", " ").replaceAll("Z", "");
				lastModificationDate = new Timestamp(format.parse(replaced).getTime());
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
	}
}
