package org.kore.kolab.notes.v3;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Tag;


public class KolabNotesParserV3Test {

    private KolabNotesParserV3 parser;

    @Before
    public void setUp() {
        parser = new KolabNotesParserV3();
    }

    @Test
    public void testParseNote() {
        InputStream inputStream = getClass().getResourceAsStream("kolab_test.xml");
        Note note = parser.parse(inputStream);

        assertEquals(Note.Classification.PUBLIC, note.getClassification());
        assertEquals("Rich Text", note.getSummary());
        assertTrue(note.getDescription().startsWith("Test description"));
        assertNotNull(note.getAuditInformation().getCreationDate());
        assertNotNull(note.getAuditInformation().getLastModificationDate());
        assertNotNull(note.getIdentification().getUid());
        assertTrue(Colors.WHITE.equals(note.getColor()));
        assertEquals("kolabnotes-provider", note.getIdentification().getProductId());
        System.out.println(note);
    }

    @Test
    public void testWriteNote() throws Exception {
        Identification identification = new Identification("599d595c-a715-4a6f-821b-c368e4cb70c2", "kolabnotes-provider");
        AuditInformation audit = new AuditInformation(new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));

        Note note = new Note(identification, audit, Note.Classification.CONFIDENTIAL, "Summary");
        note.setDescription("Beschreibung");
        note.addCategories(new Tag("Hallo"), new Tag("Servus"));
        note.setColor(Colors.BLACK);

        parser.write(note, System.out);
    }

    @Test
    public void testWriteNoteWithInlineImage() throws Exception {
        Identification identification = new Identification("599d595c-a715-4a6f-821b-c368e4cb70c2", "kolabnotes-provider");
        AuditInformation audit = new AuditInformation(new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));

        Note note = new Note(identification, audit, Note.Classification.CONFIDENTIAL, "Summary");
        note.addCategories(new Tag("Hallo"), new Tag("Servus"));
        note.setColor(Colors.BLACK);

        InputStream inputStream = getClass().getResourceAsStream("testdescription.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();

        String s;
        while ((s = reader.readLine()) != null) {
            sb.append(s);
        }

        reader.close();

        note.setDescription(sb.toString());

        parser.write(note, System.out);
    }
}
