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

import java.io.IOException;
import java.nio.file.Path;
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

    Notebook getNotebookBySummary(String summary);

    boolean deleteNotebook(String uid);

    Notebook createNotebook(String uid, String summary);

    String getRootFolder();

    KolabParser getNotesParser();

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

    /**
     * Exports a notebook as ZIP file, the name of the notebook will be the name
     * of the file. The notes will be stored in the Kolab notes storage format,
     * the filename will be the UID of the note.
     *
     * Tags will not be exported.
     *
     * @param notebook
     * @param destination - destination where the zip will be stored, the path
     * must be a folder
     * @return Path to the created ZIP
     * @throws java.io.IOException
     */
    Path exportNotebook(Notebook notebook, Path destination) throws IOException;

    /**
     * Imports a notebook from a ZIP file, the name of the ZIP file will be the
     * nae of the newly created notebook. If there is already a notebook with
     * that name, new notes will be created. The name of a note must be the UID
     * of the note, if a note with a given UID already exists, nothing will be
     * done. If there is no note, a new note will be created.
     *
     * @param zipFile
     * @return Imported notebook
     * @throws java.io.IOException
     */
    Notebook importNotebook(Path zipFile) throws IOException;

    /**
     * Exports a notebook as ZIP file, the name of the notebook will be the name
     * of the file. The notes will be stored in the format which will be parsed
     * by the given KolabParser, the filename will be the UID of the note.
     *
     * Tags will not be exported.
     *
     * @param notebook
     * @param parser
     * @param destination - destination where the zip will be stored, the path
     * must be a folder
     * @return Path to the created ZIP
     * @throws java.io.IOException
     */
    Path exportNotebook(Notebook notebook, KolabParser parser, Path destination) throws IOException;

    /**
     * Imports a notebook from a ZIP file, the name of the ZIP file will be the
     * nae of the newly created notebook. If there is already a notebook with
     * that name, new notes will be created. The name of a note must be the UID
     * of the note, if a note with a given UID already exists, nothing will be
     * done. If there is no note, a new note will be created.
     *
     * @param zipFile
     * @param parser
     * @return Import notebook
     * @throws java.io.IOException
     */
    Notebook importNotebook(Path zipFile, KolabParser parser) throws IOException;
}
