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
package org.kore.kolab.notes.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.kore.kolab.notes.Colors;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.event.EventListener;
import org.kore.kolab.notes.v3.KolabNotesParserV3;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Konrad Renner
 */
public class LocalNotesRepositoryTest {

    private final long tst = 43343434;

    @Test
    public void testPropertyChangedDeleteNotebook() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);
        Notebook mockbook = mock(Notebook.class);
        when(mockbook.getNotes()).thenReturn(Collections.EMPTY_LIST);
        when(repo.removeFromNotebookCache("UID")).thenReturn(mockbook);

        LocalNotesRepository.PropertyChangeStrategy.DELETE.performChange(repo, "UID", EventListener.Type.DELETE, "notebook", null, null);

        verify(repo).putEvent("UID", EventListener.Type.DELETE);
        verify(repo).removeFromNotebookCache("UID");
    }

    @Test
    public void testPropertyChangedDeleteNote() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);

        LocalNotesRepository.PropertyChangeStrategy.DELETE.performChange(repo, "NOTE", EventListener.Type.DELETE, "note", "UID", null);

        verify(repo).putEvent("NOTE", EventListener.Type.DELETE);
        verify(repo).removeFromNotesCache("UID", "NOTE");
        verify(repo, times(0)).removeFromNotebookCache("UID");
    }

    @Test
    public void testPropertyChangedDeleteCategorie() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);

        LocalNotesRepository.PropertyChangeStrategy.DELETE.performChange(repo, "UID", EventListener.Type.DELETE, "categories", "NOTE", null);

        verify(repo).putEvent("UID", EventListener.Type.UPDATE);
        verify(repo, times(0)).removeFromNotesCache("UID", "NOTE");
        verify(repo, times(0)).removeFromNotebookCache("UID");
    }

    @Test
    public void testPropertyChangedNewNotebook() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);
        Notebook value = mock(Notebook.class);

        LocalNotesRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "notebook", null, value);

        verify(repo).putEvent("UID", EventListener.Type.NEW);
        verify(repo).putInNotebookCache("UID", value);
    }

    @Test
    public void testPropertyChangedNewNote() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);
        Note value = mock(Note.class);

        LocalNotesRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "note", null, value);

        verify(repo).putEvent("UID", EventListener.Type.NEW);
        verify(repo).putInNotesCache("UID", value);
    }

    @Test
    public void testPropertyChangedNewCategorie() {
        LocalNotesRepository repo = mock(LocalNotesRepository.class);
        Note value = mock(Note.class);

        LocalNotesRepository.PropertyChangeStrategy.NEW.performChange(repo, "UID", EventListener.Type.NEW, "categories", null, value);

        verify(repo).putEvent("UID", EventListener.Type.UPDATE);
        verify(repo, times(0)).putInNotesCache("UID", value);
    }

    @Test
    public void testPropertyChangedValueOf() {
        assertEquals(LocalNotesRepository.PropertyChangeStrategy.DELETE, LocalNotesRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.DELETE));
        assertEquals(LocalNotesRepository.PropertyChangeStrategy.DELETE_NEW, LocalNotesRepository.PropertyChangeStrategy.valueOf(EventListener.Type.NEW, EventListener.Type.DELETE));
        assertEquals(LocalNotesRepository.PropertyChangeStrategy.NEW, LocalNotesRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.NEW));
        assertEquals(LocalNotesRepository.PropertyChangeStrategy.UPDATE, LocalNotesRepository.PropertyChangeStrategy.valueOf(null, EventListener.Type.UPDATE));
        assertEquals(LocalNotesRepository.PropertyChangeStrategy.NOTHING, LocalNotesRepository.PropertyChangeStrategy.valueOf(EventListener.Type.DELETE, EventListener.Type.UPDATE));
    }

    @Test
    public void testValueChangedChanged() {
        assertTrue(LocalNotesRepository.PropertyChangeStrategy.valueChanged("test", "Test"));
    }

    @Test
    public void testValueChangedBothNull() {
        assertFalse(LocalNotesRepository.PropertyChangeStrategy.valueChanged(null, null));
    }

    @Test
    public void testValueChangedFirstNull() {
        assertTrue(LocalNotesRepository.PropertyChangeStrategy.valueChanged(null, "Test"));
    }

    @Test
    public void testValueChangedSecondNull() {
        assertTrue(LocalNotesRepository.PropertyChangeStrategy.valueChanged("Test", null));
    }

    @Test
    public void testValueChangedNot() {
        assertFalse(LocalNotesRepository.PropertyChangeStrategy.valueChanged("test", "test"));
    }

    @Test
    public void testNoteExportImport() throws IOException {
        LocalNotesRepository repo = new LocalNotesRepository(new KolabNotesParserV3(), "Notes");

        Notebook book = repo.createNotebook("book1", "Book");
        Note createNote = book.createNote("note1", "First Note");
        createNote.setClassification(Note.Classification.CONFIDENTIAL);
        createNote.setColor(Colors.BLUE);
        createNote.setDescription("Hello World");
        createNote.getAuditInformation().setCreationDate(tst);
        createNote.getAuditInformation().setLastModificationDate(tst);

        createNote = book.createNote("note2", "Second Note");
        createNote.setDescription("Hello World 2");
        createNote.getAuditInformation().setCreationDate(tst);
        createNote.getAuditInformation().setLastModificationDate(tst);

        Path exportNotes = exportNotes(repo);

        importNotes(exportNotes, new LocalNotesRepository(new KolabNotesParserV3(), "Notes"));
    }

    @Test
    public void testNoteExportImportWithExisting() throws IOException {
        LocalNotesRepository repo = new LocalNotesRepository(new KolabNotesParserV3(), "Notes");

        Notebook book = repo.createNotebook("book1", "Book");
        Note createNote = book.createNote("note1", "First Note");
        createNote.setClassification(Note.Classification.CONFIDENTIAL);
        createNote.setColor(Colors.BLUE);
        createNote.setDescription("Hello World");

        createNote = book.createNote("note2", "Second Note");
        createNote.setDescription("Hello World 2");

        Path exportNotes = exportNotes(repo);

        repo = new LocalNotesRepository(new KolabNotesParserV3(), "Notes");

        book = repo.createNotebook("book1", "Book");
        createNote = book.createNote("note1", "First Note");
        createNote.setClassification(Note.Classification.CONFIDENTIAL);
        createNote.setColor(Colors.BLUE);
        createNote.setDescription("Hello World updated");


        importNotesExisted(exportNotes, repo, createNote);
    }

    Path exportNotes(NotesRepository repo) throws IOException {

        Path newZip = Files.createTempDirectory("test_" + Long.toString(System.currentTimeMillis()));

        Path exportNotebook = repo.exportNotebook(repo.getNotebook("book1"), newZip);

        assertNotNull(exportNotebook);

        return exportNotebook;
    }

    void importNotes(Path file, NotesRepository repo) throws IOException {
        Notebook importNotebook = repo.importNotebook(file);

        assertEquals("Book", importNotebook.getSummary());
        
        Collection<Note> notes = importNotebook.getNotes();
        
        assertTrue(notes.size() == 2);

        Note note = importNotebook.getNote("note1");
        assertEquals("First Note", note.getSummary());
        assertNotNull(note.getAuditInformation().getCreationDate().getTime());
        assertNotNull(note.getAuditInformation().getLastModificationDate().getTime());
        assertEquals(Colors.BLUE, note.getColor());
        assertEquals("Hello World", note.getDescription());
        assertEquals(Note.Classification.CONFIDENTIAL, note.getClassification());

        note = importNotebook.getNote("note2");
        assertEquals("Second Note", note.getSummary());
        assertNotNull(note.getAuditInformation().getCreationDate().getTime());
        assertNotNull(note.getAuditInformation().getLastModificationDate().getTime());
        assertNull(note.getColor());
        assertEquals("Hello World 2", note.getDescription());
        assertEquals(Note.Classification.PUBLIC, note.getClassification());

    }

    void importNotesExisted(Path file, NotesRepository repo, Note existedNote) throws IOException {

        Notebook importNotebook = repo.importNotebook(file);

        assertEquals("Book", importNotebook.getSummary());

        Collection<Note> notes = importNotebook.getNotes();

        assertTrue(notes.size() == 2);

        Note note = importNotebook.getNote("note1");
        assertEquals("First Note", note.getSummary());
        assertNotNull(note.getAuditInformation().getCreationDate().getTime());
        assertNotNull(note.getAuditInformation().getLastModificationDate().getTime());
        assertEquals(Colors.BLUE, note.getColor());
        assertEquals("Hello World updated", note.getDescription());
        assertEquals(Note.Classification.CONFIDENTIAL, note.getClassification());

        note = importNotebook.getNote("note2");
        assertEquals("Second Note", note.getSummary());
        assertNotNull(note.getAuditInformation().getCreationDate().getTime());
        assertNotNull(note.getAuditInformation().getLastModificationDate().getTime());
        assertNull(note.getColor());
        assertEquals("Hello World 2", note.getDescription());
        assertEquals(Note.Classification.PUBLIC, note.getClassification());
    }
}
