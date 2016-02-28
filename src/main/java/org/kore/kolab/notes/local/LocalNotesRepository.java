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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.KolabParser;
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
    protected final KolabParser parser;
    protected final String rootfolder;
    private boolean disableChangeListening = false;

    public LocalNotesRepository(KolabParser parser, String rootFolder) {
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
                            repo.removeFromNotesCache(oldValue.toString(), uid);
                        } else if ("categories".equalsIgnoreCase(propertyName)) {
                            correctType = EventListener.Type.UPDATE;
                        }
                        putEvent(repo, uid, correctType);
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
                        putEvent(repo, uid, correctType);
                    }

                }, UPDATE {

                    @Override
                public void performChange(LocalNotesRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                        if (valueChanged(oldValue, newValue)) {
                            putEvent(repo, uid, type);
                            Note note = repo.getNote(uid);
                            
                            if (note == null) {
                                note = repo.getNotebook(uid);
                            }
                            
                            note.getAuditInformation().setLastModificationDate(System.currentTimeMillis());
                        }
                    }

                };

        static boolean valueChanged(Object oldValue, Object newValue) {
            if (oldValue == null && newValue != null) {
                return true;
            }

            return oldValue != null && !oldValue.equals(newValue);
        }
        
        static void putEvent(LocalNotesRepository repo, String uid, Type type) {
            if (repo.getEvent(uid) == null) {
                repo.putEvent(uid, type);
            }
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
    public Notebook getNotebookBySummary(String summary) {
        initCache();
        for (Notebook nb : notebookCache.values()) {
            if (summary.equals(nb.getSummary())) {
                return nb;
            }
        }
        return null;
    }

    @Override
    public boolean deleteNotebook(String id) {
        propertyChanged(id, EventListener.Type.DELETE, "notebook", id, null);
        return notebookCache.get(id) == null;
    }

    @Override
    public Notebook createNotebook(String uid, String summary) {
        Identification identification = new Identification(uid, "kolabnotes-java");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        AuditInformation audit = new AuditInformation(now, now);
        Notebook notebook = new Notebook(identification, audit, Note.Classification.PUBLIC, summary);
        propertyChanged(uid, EventListener.Type.NEW, "notebook", null, notebook);
        notebook.addListener(this);
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
    public KolabParser getNotesParser() {
        return this.parser;
    }

    @Override
    public String getRootFolder() {
        return rootfolder;
    }
    
    @Override
    public Path exportNotebook(Notebook nb, Path destination) throws IOException {
        return exportNotebook(nb, parser, destination);
    }

    private String replacePossibleIllegalFileCharacters(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    @Override
    public Path exportNotebook(Notebook notebook, KolabParser parser, Path destination) throws IOException {
        final String fileEnding = ".xml";
        final String root = "/";
        Path tmpDirectory = Files.createTempDirectory(notebook.getIdentification().getUid());

        List<Path> xmls = new ArrayList<>();

        for (Note note : notebook.getNotes()) {
            Path pathToXML = Paths.get(tmpDirectory.toString(), note.getIdentification().getUid() + fileEnding);
            try (OutputStream os = Files.newOutputStream(pathToXML, StandardOpenOption.CREATE_NEW)) {
                parser.write(note, os);
            }

            xmls.add(pathToXML);
        }

        final Path path = Paths.get(destination.toString(), replacePossibleIllegalFileCharacters(notebook.getSummary()) + ".zip");
        final URI uri = URI.create("jar:file:" + path.toUri().getPath());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");

        try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
            for (Path xml : xmls) {
                Files.copy(xml, zipfs.getPath(root, xml.getFileName().toString()),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return path;
    }
    
    @Override
    public Notebook importNotebook(Path zipFile, final KolabParser parser) throws IOException {
        String notebookName = zipFile.getFileName().toString();
        String fileExtension = ".ZIP";
        if (notebookName.toUpperCase().endsWith(fileExtension)) {
            notebookName = notebookName.substring(0, notebookName.length() - fileExtension.length());
        }

        final Notebook book;
        Notebook existingBook = getNotebookBySummary(notebookName);
        if (existingBook == null) {
            book = createNotebook(UUID.randomUUID().toString(), notebookName);
        } else {
            book = existingBook;
        }

        try (FileSystem fs = FileSystems.newFileSystem(zipFile, null)) {
            Files.walkFileTree(fs.getPath("/"), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try (InputStream stream = Files.newInputStream(file, StandardOpenOption.READ)) {
                        Note parse = (Note) parser.parse(stream);
                        //don't change existing notes
                        if (book.getNote(parse.getIdentification().getUid()) == null) {
                            book.addNote(parse);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException ioe) {
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return book;
    }
    
    @Override
    public Notebook importNotebook(Path zipFile) throws IOException {
        return importNotebook(zipFile, parser);
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
