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

import java.util.Date;
import java.util.Map;
import org.kore.kolab.notes.event.EventListener;

/**
 *
 * @author Konrad Renner
 */
public interface RemoteNotesRepository extends NotesRepository {

    /**
     * This method refreshes the local cache with data from a remote server.
     * Note: local changes will be discarded!
     * @param listener
     */
    void refresh(Listener... listener);

    /**
     * This method refreshes the local cache with data from a remote server.
     * Note: local changes will be discarded! If a note was altered/created
     * before or on the given modification date, the note will not be filled
     * with data. So it is possible to load just notes completly (with kolab.xml
     * parsing) which were altered/created after the given date. The summary of
     * such notes is "NOT_LOADED".
     *
     * @param modificationDate
     * @param listener
     */
    void refresh(Date modificationDate, Listener... listener);

    /**
     * Sends tracked changes to the remote server. Note: Changes are just
     * tracked, if the changes are made on objects which "live" in the the
     * current instance of the Repository. See also method merge(Map)
     *
     * @param listener
     * @see RemoteNotesRepository.merge(Map)
     */
    void merge(Listener... listener);

    /**
     * Sends tracked and given changes to the remote server. This method should
     * be used, if there are changes which were not tracked by this repository,
     * but you want to send these changes to the server
     *
     * @param eventTypes
     * @param listener
     */
    void merge(Map<String, EventListener.Type> eventTypes, Listener... listener);

    /**
     * Fills an unloaded note with data from the given one. If the note is not
     * found, or loaded, nothing will be done
     *
     * @param note
     */
    void fillUnloadedNote(Note note);

    /**
     * Checks if a note is completely loaded from a server, after a refresh
     *
     * @param note
     * @return true if note ist completely loaded from server
     */
    boolean noteCompletelyLoaded(Note note);

    interface Listener {

        void onSyncUpdate(String folderName);

        void onFolderSyncException(String folderName, Exception e);
    }
}
