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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.event.EventListener;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepositoryTest {

    private ImapRepository imapRepository;

    @Before
    public void setUp() {
        AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
        imapRepository = new ImapRepository(new KolabNotesParserV3(), info, "Notes");

        createTestdata();
    }

    @Test
    public void testPropertyChangedDeleteNotebook() {
        ImapRepository repo = mock(ImapRepository.class);
        
        verify(repo).putEvent("UID", EventListener.Type.DELETE);
        verify(repo).removeFromNotebookCache("UID");

        ImapRepository.PropertyChangeStrategy.DELETE.performChange(repo, "UID", EventListener.Type.DELETE, "notebook", null, null);
    }

    @Test
    public void testPropertyChangedDeleteNote() {
        ImapRepository repo = mock(ImapRepository.class);

        verify(repo).putEvent("UID", EventListener.Type.DELETE);
        verify(repo).removeFromNotesCache("UID", "NOTE");
        verify(repo, times(0)).removeFromNotebookCache("UID");

        ImapRepository.PropertyChangeStrategy.DELETE.performChange(repo, "UID", EventListener.Type.DELETE, "note", "NOTE", null);
    }

    @Test
    public void testPropertyChangedDeleteCategorie() {
        ImapRepository repo = mock(ImapRepository.class);

        verify(repo).putEvent("UID", EventListener.Type.UPDATE);
        verify(repo, times(0)).removeFromNotesCache("UID", "NOTE");
        verify(repo, times(0)).removeFromNotebookCache("UID");

        ImapRepository.PropertyChangeStrategy.DELETE.performChange(repo, "UID", EventListener.Type.DELETE, "categories", "NOTE", null);
    }

    @Test
    public void testPropertyChangedNewNotebook() {
        ImapRepository repo = mock(ImapRepository.class);
        Notebook value = mock(Notebook.class);

        verify(repo).putEvent("UID", EventListener.Type.NEW);
        verify(repo).putInNotebookCache("UID", value);

        ImapRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "notebook", null, value);
    }

    @Test
    public void testPropertyChangedNewNote() {
        ImapRepository repo = mock(ImapRepository.class);
        Note value = mock(Note.class);

        verify(repo).putEvent("UID", EventListener.Type.NEW);
        verify(repo).putInNotesCache("UID", value);

        ImapRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "note", null, value);
    }

    @Test
    public void testPropertyChangedNewCategorie() {
        ImapRepository repo = mock(ImapRepository.class);
        Note value = mock(Note.class);

        verify(repo).putEvent("UID", EventListener.Type.UPDATE);
        verify(repo, times(0)).putInNotesCache("UID", value);

        ImapRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "categories", null, value);
    }

    @Test
    public void testPropertyChangedValueOf() {
        assertEquals(ImapRepository.PropertyChangeStrategy.DELETE, ImapRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.DELETE));
        assertEquals(ImapRepository.PropertyChangeStrategy.DELETE_NEW, ImapRepository.PropertyChangeStrategy.valueOf(EventListener.Type.NEW, EventListener.Type.DELETE));
        assertEquals(ImapRepository.PropertyChangeStrategy.NEW, ImapRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.NEW));
        assertEquals(ImapRepository.PropertyChangeStrategy.UPDATE, ImapRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.UPDATE));
        assertEquals(ImapRepository.PropertyChangeStrategy.NOTHING, ImapRepository.PropertyChangeStrategy.valueOf(EventListener.Type.DELETE, EventListener.Type.UPDATE));
    }
    
    @Test
    public void testValueChangedChanged() {
        assertTrue(ImapRepository.PropertyChangeStrategy.valueChanged("test", "Test"));
    }

    @Test
    public void testValueChangedBothNull() {
        assertFalse(ImapRepository.PropertyChangeStrategy.valueChanged(null, null));
    }

    @Test
    public void testValueChangedFirstNull() {
        assertTrue(ImapRepository.PropertyChangeStrategy.valueChanged(null, "Test"));
    }

    @Test
    public void testValueChangedSecondNull() {
        assertTrue(ImapRepository.PropertyChangeStrategy.valueChanged("Test", null));
    }

    @Test
    public void testValueChangedNot() {
        assertFalse(ImapRepository.PropertyChangeStrategy.valueChanged("test", "test"));
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
    }

    @Test
    public void testRefresh() {
    }

    @Test
    public void testMerge() {
    }

    @Test
    public void testSetKolabXML() throws Exception {
    }

    @Test
    public void testFindMessage() throws Exception {
    }

    @Test
    public void testInitCache() {
    }

    @Test
    public void testInitNotesFromFolder() throws Exception {
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
