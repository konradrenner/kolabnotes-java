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

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kore.kolab.notes.KolabNotesParser;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.event.EventListener;

/**
 *
 * @author Konrad Renner
 */
public class LocalNotesRepository implements Serializable, NotesRepository, EventListener {

    protected final Map<String, EventListener.Type> eventCache;
    protected final Map<String, Notebook> notebookCache;
    protected final Map<String, Note> notesCache;
    protected final Map<String, Notebook> deletedNotebookCache;
    protected final Map<String, Map<String, Note>> deletedNotesCache;
    protected final KolabNotesParser parser;
    protected final String rootfolder;
    private boolean disableChangeListening = false;

    public LocalNotesRepository(KolabNotesParser parser, String rootFolder) {
        this.notebookCache = new ConcurrentHashMap<String, Notebook>();
        this.notesCache = new ConcurrentHashMap<String, Note>();
        this.deletedNotebookCache = new ConcurrentHashMap<String, Notebook>();
        this.deletedNotesCache = new ConcurrentHashMap<String, Map<String, Note>>();
        this.eventCache = new ConcurrentHashMap<String, EventListener.Type>();
        this.parser = parser;
        this.rootfolder = rootFolder;
    }

    protected void disableChangeListening() {
        this.disableChangeListening = true;
    }

    protected void enableChangeListening() {
        this.disableChangeListening = false;
    }

    protected boolean isChangeListeningDisabled() {
        return disableChangeListening;
    }

    @Override
    public Map<String, Type> getTrackedChanges() {
        return Collections.unmodifiableMap(eventCache);
    }

    @Override
    public void trackExisitingNotebooks(Collection<Notebook> existing) {
        for (Notebook nb : existing) {
            nb.addListener(this);
            putInNotebookCache(nb.getIdentification().getUid(), nb);

            for (Note note : nb.getNotes()) {
                note.addListener(this);
                putInNotesCache(note.getIdentification().getUid(), note);
            }
        }
    }

    enum PropertyChangeStrategy {

        NOTHING {

                    @Override
            public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        //Do nothing
                    }

                },
        DELETE_NEW {

                    @Override
            public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        //if a newly created element should be removed, there must  be no changes sent to the server
                        repo.removeEvent(uid);
                    }

                }, DELETE {

                    @Override
                public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        EventListener.Type correctType = type;
                        if ("notebook".equalsIgnoreCase(propertyName)) {
                            Notebook removed = repo.removeFromNotebookCache(uid);
                            //Remove all notes also
                            for (Note note : removed.getNotes()) {
                                repo.removeFromNotesCache(uid, note.getIdentification().getUid());
                            }
                        } else if ("note".equalsIgnoreCase(propertyName)) {
                            repo.removeFromNotesCache(uid, oldValue.toString());
                        } else if ("categories".equalsIgnoreCase(propertyName)) {
                            correctType = EventListener.Type.UPDATE;
                        }
                        repo.putEvent(uid, correctType);
                    }

                }, NEW {

                    @Override
                public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        EventListener.Type correctType = type;
                        if ("notebook".equalsIgnoreCase(propertyName)) {
                            repo.putInNotebookCache(uid, (Notebook) newValue);
                        } else if ("note".equalsIgnoreCase(propertyName)) {
                            repo.putInNotesCache(uid, (Note) newValue);
                        } else if ("categories".equalsIgnoreCase(propertyName)) {
                            correctType = EventListener.Type.UPDATE;
                        }
                        repo.putEvent(uid, correctType);
                    }

                }, UPDATE {

                    @Override
                public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        if (valueChanged(oldValue, newValue)) {
                            repo.putEvent(uid, type);
                        }
                    }

                };

        static boolean valueChanged(Object oldValue, Object newValue) {
            if (oldValue == null && newValue != null) {
                return true;
            }

            return oldValue != null && !oldValue.equals(newValue);
        }

        static PropertyChangeStrategy valueOf(Type existingtype, Type newChangeType) {
            if (existingtype == EventListener.Type.NEW && newChangeType == EventListener.Type.DELETE) {
                return DELETE_NEW;
            } else if (newChangeType == EventListener.Type.DELETE) {
                return DELETE;
            } else if (newChangeType == EventListener.Type.NEW) {
                return NEW;
            } else if (existingtype == null) {
                return UPDATE;
            }
            return NOTHING;
        }

        abstract void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue);
    }

    @Override
    public void propertyChanged(String uid, Type type, String propertyName, Object oldValue, Object newValue) {
        if (disableChangeListening) {
            return;
        }

        EventListener.Type eventType = eventCache.get(uid);

        PropertyChangeStrategy.valueOf(eventType, type).performChange(this, uid, type, propertyName, oldValue, newValue);
    }

    public EventListener.Type getEvent(String uid) {
        return eventCache.get(uid);
    }

    protected void initCache() {
        //nothing at the moment
    }

    @Override
    public Note getNote(String id) {
        initCache();
        return notesCache.get(id);
    }

    @Override
    public Collection<Note> getNotes() {
        initCache();
        return Collections.unmodifiableCollection(notesCache.values());
    }

    @Override
    public Collection<Notebook> getNotebooks() {
        initCache();
        return Collections.unmodifiableCollection(notebookCache.values());
    }

    @Override
    public Notebook getNotebook(String uid) {
        initCache();
        return notebookCache.get(uid);
    }

    @Override
    public boolean deleteNotebook(String id) {
        propertyChanged(id, EventListener.Type.DELETE, "notebook", id, null);
        return notebookCache.get(id) == null;
    }

    @Override
    public Notebook createNotebook(String uid, String summary) {
        Note.Identification identification = new Note.Identification(uid, "kolabnotes-java");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Note.AuditInformation audit = new Note.AuditInformation(now, now);
        Notebook notebook = new Notebook(identification, audit, Note.Classification.PUBLIC, summary);
        propertyChanged(uid, EventListener.Type.NEW, "notebook", null, notebook);
        return notebook;
    }

    protected void addNotebook(String uid, Notebook notebook) {
        notebookCache.put(uid, notebook);
        notebook.addListener(this);
    }

    protected void addNote(String uid, Note note) {
        notesCache.put(uid, note);
        note.addListener(this);
    }

    protected Notebook removeFromNotebookCache(String uid) {
        Notebook remove = notebookCache.remove(uid);
        if (remove != null) {
            deletedNotebookCache.put(uid, remove);
        }
        return remove;
    }

    protected void removeFromNotesCache(String uidNotebook, String uidNote) {
        Note remove = notesCache.remove(uidNote);
        if (remove != null) {
            Map<String, Note> book = deletedNotesCache.get(uidNotebook);

            if (book == null) {
                book = new ConcurrentHashMap<String, Note>();
                deletedNotesCache.put(uidNotebook, book);
            }
            book.put(uidNote, remove);
        }
    }

    protected void putInNotebookCache(String uid, Notebook value) {
        notebookCache.put(uid, value);
    }

    protected void putInNotesCache(String uid, Note value) {
        notesCache.put(uid, value);
    }

    protected void removeEvent(String uid) {
        eventCache.remove(uid);
    }

    protected void putEvent(String uid, Type type) {
        eventCache.put(uid, type);
    }

    @Override
    public KolabNotesParser getNotesParser() {
        return this.parser;
    }

    @Override
    public String getRootFolder() {
        return rootfolder;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.rootfolder != null ? this.rootfolder.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LocalNotesRepository other = (LocalNotesRepository) obj;
        if ((this.rootfolder == null) ? (other.rootfolder != null) : !this.rootfolder.equals(other.rootfolder)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LocalNotesRepository{" + "eventCache=" + eventCache + ", notebookCache=" + notebookCache + ", notesCache=" + notesCache + ", deletedNotebookCache=" + deletedNotebookCache + ", deletedNotesCache=" + deletedNotesCache + ", parser=" + parser + ", rootfolder=" + rootfolder + ", disableChangeListening=" + disableChangeListening + '}';
    }
}
