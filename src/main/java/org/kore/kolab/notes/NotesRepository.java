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

import java.util.Collection;
import java.util.Map;
import org.kore.kolab.notes.event.EventListener;

/**
 *
 * @author Konrad Renner
 */
public interface NotesRepository {

    /**
     * Gets a note with the given UID
     *
     * @param uid
     * @return Note
     */
    Note getNote(String uid);

    /**
     * Gets all Notes from the repository
     *
     * @return Collection
     */
    Collection<Note> getNotes();

    Collection<Notebook> getNotebooks();

    Notebook getNotebook(String uid);

    boolean deleteNotebook(String uid);

    Notebook createNotebook(String uid, String summary);

    /**
     * Tracks existing notebooks, e.g. from another Repository. This method can
     * also be used, if you want to initialize the repository with data. Note:
     * The given notebooks will referenced, not copied!
     *
     * @param existing
     */
    void trackExisitingNotebooks(Collection<Notebook> existing);

    /**
     * Returns an unmodifyable map, with the type of a change per UID of a
     * notebook or note
     *
     * @return Map
     */
    Map<String, EventListener.Type> getTrackedChanges();
}
