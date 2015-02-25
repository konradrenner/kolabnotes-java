/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kore.kolab.notes.imap;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.KolabNotesParser;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.event.EventListener;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepository implements RemoteNotesRepository, EventListener, Serializable {

    private final static String KOLAB_TEXT = "This is a Kolab Groupware object.\n"
            + "To view this object you will need a Kolab Groupware Client.\n"
            + "For a list of Kolab Groupware Clients please visit:\n"
            + "http://www.kolab.org/get-kolab";

    private final Map<String, EventListener.Type> eventCache;
    private final Map<String, Notebook> notebookCache;
    private final Map<String, Note> notesCache;
    private final Map<String, Notebook> deletedNotebookCache;
    private final Map<String, Map<String, Note>> deletedNotesCache;
    private final KolabNotesParser parser;
    private final AccountInformation account;
    private final String rootfolder;
    private boolean disableChangeListening = false;

    public ImapRepository(KolabNotesParser parser, AccountInformation account, String rootFolder) {
        this.notebookCache = new ConcurrentHashMap<String, Notebook>();
        this.notesCache = new ConcurrentHashMap<String, Note>();
        this.deletedNotebookCache = new ConcurrentHashMap<String, Notebook>();
        this.deletedNotesCache = new ConcurrentHashMap<String, Map<String, Note>>();
        this.eventCache = new ConcurrentHashMap<String, EventListener.Type>();
        this.parser = parser;
        this.account = account;
        this.rootfolder = rootFolder;
    }

    @Override
    public Map<String, Type> getTrackedChanges() {
        return Collections.unmodifiableMap(eventCache);
    }

    @Override
    public void merge(Map<String, Type> eventTypes) {
        eventCache.putAll(eventTypes);
        merge();
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
            public void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                //Do nothing
            }

        },
        DELETE_NEW {

            @Override
            public void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
                //if a newly created element should be removed, there must  be no changes sent to the server
                repo.removeEvent(uid);
            }

        }, DELETE {

            @Override
            public void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
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
            public void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
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
            public void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue) {
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

        abstract void performChange(ImapRepository repo, String uid, Type type, String propertyName, Object oldValue, Object newValue);
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

    @Override
    public void refresh() {
        notesCache.clear();
        notebookCache.clear();
        try {
            Properties props = new Properties();

            Session session = Session.getInstance(props, null);

            Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imap");

            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            Folder rFolder = store.getFolder(rootfolder);
            initNotesFromFolder(rFolder);

            Folder[] allFolders = rFolder.list("*");

            for (Folder folder : allFolders) {
                initNotesFromFolder(folder);
            }

            store.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void merge() {
        initCache();
        disableChangeListening = true;
        try {
            Properties props = new Properties();

            Session session = Session.getInstance(props, null);

            Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imap");

            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            //Actual there are no notebooks in notebooks supported
            ArrayList<Notebook> notebooks = new ArrayList<Notebook>(getNotebooks());
            //Deleted notebooks must be merged with the server too (delete from server)
            notebooks.addAll(deletedNotebookCache.values());
            for (Notebook book : notebooks) {
                Folder folder = store.getFolder(book.getSummary());

                Type event = getEvent(book.getIdentification().getUid());
                if (event != null) {
                    if (folder.exists()) {
                        if (event == Type.DELETE) {
                            folder.open(Folder.READ_WRITE);
                            folder.delete(true);
                        } else if (event == Type.UPDATE) {
                            folder.renameTo(store.getFolder(book.getSummary()));
                        }

                    } else {
                        folder.create(Folder.HOLDS_MESSAGES);

                        if (event == Type.UPDATE || event == Type.NEW) {
                            folder.renameTo(store.getFolder(book.getSummary()));
                        } 
                    }
                }

                //if the folder does not exist, do nothing
                if (folder.exists()) {
                    if (!folder.isOpen()) {
                        folder.open(Folder.READ_WRITE);
                    }

                    ArrayList<Note> notes = new ArrayList<Note>(book.getNotes());
                    Map<String, Note> deletedNotes = deletedNotesCache.get(book.getIdentification().getUid());
                    if (deletedNotes != null) {
                        notes.addAll(deletedNotes.values());
                    }
                    for (Note note : notes) {
                        Message[] messages = folder.getMessages();

                        event = getEvent(note.getIdentification().getUid());
                        if (event == Type.NEW) {
                            MimeMessage message = new MimeMessage(Session.getInstance(System.getProperties()));
                            message.setHeader("version", "3.0");
                            message.setFrom(new InternetAddress(account.getUsername()));
                            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.getUsername()));
                            message.setSubject(note.getIdentification().getUid());
                            // create the message part 
                            MimeBodyPart content = new MimeBodyPart();
                            // fill message
                            content.setText(KOLAB_TEXT);
                            content.setFileName("kolab.xml");
                            Multipart multipart = new MimeMultipart();
                            multipart.addBodyPart(content);

                            message.setContent(multipart);
                            setKolabXML(note, multipart);
                        } else if (event == Type.UPDATE) {
                            Timestamp now = new Timestamp(System.currentTimeMillis());
                            String uid = note.getIdentification().getUid();
                            Note.Identification id = new Note.Identification(uid, "kolabnotes-java");
                            Note.AuditInformation audit = new Note.AuditInformation(now, now);
                            Note newNote = new Note(id, audit, Note.Classification.PUBLIC, note.getSummary());

                            Message message = findMessage(uid, messages);

                            setKolabXML(note, (Multipart) message.getContent());
                        } else if (event == Type.DELETE) {
                            Message message = findMessage(note.getIdentification().getUid(), messages);
                            if (message != null) {
                                Flags deleted = new Flags(Flags.Flag.DELETED);
                                folder.setFlags(new Message[]{message}, deleted, true);
                            }
                        }
                    }
                }
            }

            store.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            disableChangeListening = false;
        }
    }

    void setKolabXML(Note note, Multipart content) throws Exception {
        for (int i = 0; i < content.getCount(); i++) {
            BodyPart bodyPart = content.getBodyPart(i);
            if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                bodyPart.setDataHandler(null);
                bodyPart.setContent(note, "APPLICATION/VND.KOLAB+XML");
            }
        }
    }

    Message findMessage(String uid, Message[] messages) throws Exception {
        for (Message m : messages) {
            Multipart content = (Multipart) m.getContent();
            for (int i = 0; i < content.getCount(); i++) {
                BodyPart bodyPart = content.getBodyPart(i);
                if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                    Note note = parser.parseNote(bodyPart.getInputStream());
                    if (uid.equals(note.getIdentification().getUid())) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    void initCache(){
        if (notesCache.isEmpty()) {
            refresh();
        }
    }

    void initNotesFromFolder(Folder folder) throws MessagingException, IOException {
        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.getMessages();

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Note.Identification id = new Note.Identification(Long.toString(System.currentTimeMillis()), "kolabnotes-java");
        Note.AuditInformation audit = new Note.AuditInformation(now, now);

        Notebook notebook = new Notebook(id, audit, Note.Classification.PUBLIC, folder.getName());
        addNotebook(notebook.getIdentification().getUid(), notebook);
        
        for (Message m : messages) {
            Multipart content = (Multipart) m.getContent();
            for (int i = 0; i < content.getCount(); i++) {
                BodyPart bodyPart = content.getBodyPart(i);
                if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                    Note note = parser.parseNote(bodyPart.getInputStream());
                    notebook.addNote(note);
                    addNote(note.getIdentification().getUid(), note);
                }
            }
        }
    }

    void addNotebook(String uid, Notebook notebook) {
        notebookCache.put(uid, notebook);
    }

    void addNote(String uid, Note note) {
        notesCache.put(uid, note);
    }

    Notebook removeFromNotebookCache(String uid) {
        Notebook remove = notebookCache.remove(uid);
        if (remove != null) {
            deletedNotebookCache.put(uid, remove);
        }
        return remove;
    }

    void removeFromNotesCache(String uidNotebook, String uidNote) {
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

    void putInNotebookCache(String uid, Notebook value) {
        notebookCache.put(uid, value);
    }

    void putInNotesCache(String uid, Note value) {
        notesCache.put(uid, value);
    }

    void removeEvent(String uid) {
        eventCache.remove(uid);
    }

    void putEvent(String uid, Type type) {
        eventCache.put(uid, type);
    }
}
