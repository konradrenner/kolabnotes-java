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
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.event.EventListener;
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
        imapRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, "Notes");

        //createTestdata();
    }

    @Test
    public void testChange() {
        imapRepository.refresh();
        Notebook nb = imapRepository.createNotebook(UUID.randomUUID().toString(), "Testbook");
        Note createNote = nb.createNote(UUID.randomUUID().toString(), "Testnote");
        createNote.setClassification(Note.Classification.PRIVATE);
        createNote.setDescription("the description");
        createNote.addCategories("Work");
        
        imapRepository.merge();
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

        assertEquals("Summary", createNote.getSummary());
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewNoteUID"));
    }

    @Ignore
    @Test
    public void testRefresh() {
        //This test could fail, it depends on the server settings and of course on the notes on the server!
        imapRepository.refresh();
        Collection<Notebook> notebooks = imapRepository.getNotebooks();

        System.out.println(notebooks);
        assertFalse(notebooks.isEmpty());
    }

    @Ignore
    @Test
    public void testMerge() {
        Notebook createNotebook = imapRepository.createNotebook("Testingbook-UID", "Testbook");
        Note note = createNotebook.createNote("Testingnote-UID", "Testingnote");
        note.setDescription("Beschreibung");
        note.addCategories("Work", "Family");
        note.setClassification(Note.Classification.CONFIDENTIAL);

        imapRepository.merge();
    }

    void createTestdata() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Note.Identification ident = new Note.Identification("bookOne", "kolabnotes-java");
        Note.AuditInformation audit = new Note.AuditInformation(now, now);
        Notebook book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book One");
        Note note = book.createNote("bookOnenoteOne", "Note one");
        note.setDescription("This is note one of book one");
        note.setClassification(Note.Classification.CONFIDENTIAL);
        note.addCategories("TEST", "WORK");
        note.addListener(imapRepository);
        imapRepository.addNote("bookOnenoteOne", note);
        note = book.createNote("bookOnenoteTwo", "Note two");
        imapRepository.addNote("bookOnenoteTwo", note);
        imapRepository.addNotebook("bookOne", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Note.Identification("bookTwo", "kolabnotes-java");
        audit = new Note.AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PRIVATE, "Book Two");
        note = book.createNote("bookTwonoteOne", "Note one");
        imapRepository.addNote("bookTwonoteOne", note);
        imapRepository.addNotebook("bookTwo", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Note.Identification("bookThree", "kolabnotes-java");
        audit = new Note.AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book Three");
        imapRepository.addNotebook("bookThree", book);
        book.addListener(imapRepository);

        ident = new Note.Identification("Notes", "kolabnotes-java");
        audit = new Note.AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Notes");
        imapRepository.addNotebook("Notes", book);
        book.addListener(imapRepository);
    }
}
