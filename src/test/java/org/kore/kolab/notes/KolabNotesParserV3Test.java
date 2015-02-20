package org.kore.kolab.notes;

import java.io.InputStream;
import java.sql.Timestamp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kore.kolab.notes.v3.KolabNotesParserV3;


public class KolabNotesParserV3Test {

	private KolabNotesParserV3 parser;

	@Before
	public void setUp() {
		parser = new KolabNotesParserV3();
	}

	@Test
	public void testParseNote() {
		InputStream inputStream = getClass().getResourceAsStream("kolab_test.xml");
        Note note = (Note) parser.parseNote(inputStream);

        assertEquals(Note.Classification.PUBLIC, note.getClassification());
		assertEquals("Rich Text", note.getSummary());
            assertTrue(note.getDescription().startsWith("Test description"));
		assertNotNull(note.getAuditInformation().getCreationDate());
		assertNotNull(note.getAuditInformation().getLastModificationDate());
		assertNotNull(note.getIdentification().getUid());
		assertEquals("kolabnotes-provider", note.getIdentification().getProductId());
	}

    @Ignore
	@Test
	public void testWriteNote() throws Exception {
		Note.Identification identification = new Note.Identification("599d595c-a715-4a6f-821b-c368e4cb70c2","kolabnotes-provider");
		Note.AuditInformation audit = new Note.AuditInformation(	new Timestamp(System.currentTimeMillis()),
														new Timestamp(System.currentTimeMillis()));

		Note note = new Note(identification, audit, Note.Classification.CONFIDENTIAL, "Summary");
		note.setDescription("Beschreibung");
		note.addCategories("Hallo", "Servus");

		parser.writeNote(note, System.out);
	}

}
