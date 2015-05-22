/*
 * Copyright (C) 2015 Konrad Renner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kore.kolab.notes.imap;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.event.EventListener;
import org.kore.kolab.notes.v3.KolabConfigurationParserV3;
import org.kore.kolab.notes.v3.KolabNotesParserV3;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepositoryTest {

    private ImapNotesRepository imapRepository;

    @Before
    public void setUp() {
        AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
        imapRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, "Notes", new KolabConfigurationParserV3());

        createTestdata();
    }

    @Test
    public void testGetNote() {
        Note note = imapRepository.getNote("bookOnenoteOne");

        assertEquals("Note one", note.getSummary());
    }

    @Test
    public void testGetNotes() {
        Collection<Note> notes = imapRepository.getNotes();

        assertTrue(notes.size() == 3);
    }

    @Test
    public void testGetNotebooks() {
        Collection<Notebook> notebooks = imapRepository.getNotebooks();

        assertTrue(notebooks.size() == 4);
    }

    @Test
    public void testGetNotebook() {
        Notebook notebook = imapRepository.getNotebook("Notes");

        assertEquals("Notes", notebook.getSummary());
    }

    @Test
    public void testDeleteNotebook() {
        boolean deleted = imapRepository.deleteNotebook("Notes");

        assertTrue(deleted);
        assertNull(imapRepository.getNotebook("Notes"));
        assertEquals(EventListener.Type.DELETE, imapRepository.getEvent("Notes"));
    }

    @Test
    public void testCreateNotebook() {
        Notebook createNotebook = imapRepository.createNotebook("NewBookUID", "Cool New Book");
        
        assertEquals("Cool New Book", createNotebook.getSummary());
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewBookUID"));
    }
    
    @Test
    public void testCreateNote() {
        Note createNote = imapRepository.createNotebook("NewBookUID", "Cool New Book").createNote("NewNoteUID", "Summary");
        createNote.addCategories(new Tag("Work"));

        assertEquals("Summary", createNote.getSummary());
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewBookUID"));
        assertEquals(createNote.getCategories().iterator().next(), new Tag("Work"));
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewNoteUID"));
		assertEquals(createNote, imapRepository.getNote(createNote.getIdentification().getUid()));
    }

    @Test
    public void testUpdateNote() {
        Note note = imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne");
        Timestamp lastModificationDate = note.getAuditInformation().getLastModificationDate();
	note.setSummary("Hallo");

	assertEquals("Hallo", imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne").getSummary());
        assertEquals(EventListener.Type.UPDATE, imapRepository.getEvent("bookOnenoteOne"));
        assertTrue(lastModificationDate.before(note.getAuditInformation().getLastModificationDate()));
    }

    @Test
    public void testUpdateNoteLoadedRepository() {
	Note note = imapRepository.getNote("bookOnenoteOne");
	note.setSummary("Hallo");

	assertEquals("Hallo", imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne").getSummary());
	assertEquals(EventListener.Type.UPDATE, imapRepository.getEvent("bookOnenoteOne"));
    }

    @Test
    public void testDeleteNote() {
	imapRepository.getNotebook("bookOne").deleteNote("bookOnenoteOne");

	assertNull(imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne"));
	assertEquals(EventListener.Type.DELETE, imapRepository.getEvent("bookOnenoteOne"));
    }

    @Ignore
    @Test
    public void testRemoteChange() {
        imapRepository.refresh();
        Notebook nb = imapRepository.getNotebookBySummary("Testbook");
        Note createNote = nb.createNote(UUID.randomUUID().toString(), "kolabnotes-java note");
        createNote.setDescription("some text");
        createNote.addCategories(new Tag("Java"));
        //nb.createNote(UUID.randomUUID().toString(), "Testnote2");
        //nb.createNote(UUID.randomUUID().toString(), "Neuer Versuchnotiz").setDescription("Testbeschreibung");
        //nb.deleteNote("717f5a89-bf9d-44b8-b1d7-2068c5a2a1f6");
        //Notebook nb = imapRepository.getNotebookBySummary("Kolabnotes");
//        Note createNote = nb.createNote(UUID.randomUUID().toString(), "Testnote");
        //Note createNote = nb.getNote("727c41fc-ec28-11e4-92d0-525477715fa2");
//        createNote.setClassification(Note.Classification.PRIVATE);
//        createNote.setDescription("the description");
//        createNote.addCategories("Linux");
        
        imapRepository.merge();
    }

    @Ignore
    @Test
    public void testRefresh() {
        //This test could fail, it depends on the server settings and of course on the notes on the server!
        imapRepository.refresh();
        Collection<Notebook> notebooks = imapRepository.getNotebooks();

        System.out.println(notebooks);
        assertTrue(imapRepository.getTrackedChanges().isEmpty());
        assertFalse(notebooks.isEmpty());
    }

    @Ignore
    @Test
    public void testMerge() {
        Notebook createNotebook = imapRepository.createNotebook("Testingbook-UID", "Testbook");
        Note note = createNotebook.createNote("Testingnote-UID", "Testingnote");
        note.setDescription("Beschreibung");
        note.addCategories(new Tag("Work"));
        note.setClassification(Note.Classification.CONFIDENTIAL);

        imapRepository.merge();
    }

    void createTestdata() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.YEAR, 2014);
        Timestamp now = new Timestamp(cal.getTimeInMillis());
        Identification ident = new Identification("bookOne", "kolabnotes-java");
        AuditInformation audit = new AuditInformation(now, now);
        Notebook book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book One");
        Note note = book.createNote("bookOnenoteOne", "Note one");
        note.setDescription("This is note one of book one");
        note.setClassification(Note.Classification.CONFIDENTIAL);
        note.addCategories(new Tag("TEST"), new Tag("WORK"));
        note.addListener(imapRepository);
        note.getAuditInformation().setLastModificationDate(cal.getTimeInMillis());
        imapRepository.addNote("bookOnenoteOne", note);
        note = book.createNote("bookOnenoteTwo", "Note two");
        imapRepository.addNote("bookOnenoteTwo", note);
        imapRepository.addNotebook("bookOne", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Identification("bookTwo", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PRIVATE, "Book Two");
        note = book.createNote("bookTwonoteOne", "Note one");
        imapRepository.addNote("bookTwonoteOne", note);
        imapRepository.addNotebook("bookTwo", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Identification("bookThree", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book Three");
        imapRepository.addNotebook("bookThree", book);
        book.addListener(imapRepository);

        ident = new Identification("Notes", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Notes");
        imapRepository.addNotebook("Notes", book);
        book.addListener(imapRepository);
    }
}
