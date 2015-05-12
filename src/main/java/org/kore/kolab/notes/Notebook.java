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
package org.kore.kolab.notes;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.kore.kolab.notes.event.EventListener;

/**
 * Represents a notebook
 *
 * @author Konrad Renner
 */
public class Notebook extends Note {

    //Key is the UID of the note
    private final Map<String, Note> notes;

    public Notebook(Identification identification, AuditInformation auditInformation, Classification classification, String summary) {
        super(identification, auditInformation, classification, summary);
        this.notes = new LinkedHashMap<String, Note>();
    }

    public Collection<Note> getNotes() {
        return Collections.unmodifiableCollection(notes.values());
    }

    public Note getNote(String uid) {
        return notes.get(uid);
    }

    public Note createNote(String uid, String summary) {
        Identification identification = new Identification(uid, "kolabnotes-java");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        AuditInformation audit = new AuditInformation(now, now);
        Note note = new Note(identification, audit, Classification.PUBLIC, summary);
        List<EventListener> eventListener = getEventListener();
        note.addListener(eventListener.toArray(new EventListener[eventListener.size()]));
        addNote(note);
        return note;
    }
    
    public void addNote(Note note) {
        firePropertyChange(note.getIdentification().getUid(), EventListener.Type.NEW, "note", null, note);
        notes.put(note.getIdentification().getUid(), note);
    }

    public void deleteNote(String uid) {
        firePropertyChange(uid, EventListener.Type.DELETE, "note", getIdentification().getUid(), null);
        notes.remove(uid);
    }

    @Override
    public String toString() {
        return "Notebook{" + super.toString() + "notes=" + notes + '}';
    }

}
