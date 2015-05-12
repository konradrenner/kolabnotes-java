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

import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.event.EventListener;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * @author Konrad Renner
 */
public class LocalNotesRepositoryTest {
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

}
