/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kore.kolab.notes.imap;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import korex.mail.BodyPart;
import korex.mail.Flags;
import korex.mail.Folder;
import korex.mail.Message;
import korex.mail.MessagingException;
import korex.mail.Multipart;
import korex.mail.Session;
import korex.mail.Store;
import korex.mail.internet.InternetAddress;
import korex.mail.internet.MimeBodyPart;
import korex.mail.internet.MimeMessage;
import korex.mail.internet.MimeMultipart;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.KolabNotesParser;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.local.LocalNotesRepository;

/**
 *
 * @author Konrad Renner
 */
public class ImapNotesRepository extends LocalNotesRepository implements RemoteNotesRepository {

    private final static String KOLAB_TEXT = "This is a Kolab Groupware object.\n"
            + "To view this object you will need a Kolab Groupware Client.\n"
            + "For a list of Kolab Groupware Clients please visit:\n"
            + "http://www.kolab.org/get-kolab";

    private final AccountInformation account;

    public ImapNotesRepository(KolabNotesParser parser, AccountInformation account, String rootFolder) {
        super(parser, rootFolder);
        this.account = account;
    }

    @Override
    public void merge(Map<String, Type> eventTypes, Listener... listener) {
        eventCache.putAll(eventTypes);
        merge(listener);
    }

    @Override
    public void refresh(Listener... listener) {
        notesCache.clear();
        notebookCache.clear();
        try {
            Properties props = new Properties();

            Session session = Session.getInstance(props, null);

            Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imap");

            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            Folder rFolder = store.getFolder(rootfolder);
            initNotesFromFolder(rFolder);

            Folder[] allFolders = rFolder.list("Kolabnotes");

            for (Folder folder : allFolders) {
                initNotesFromFolder(folder);
                
                for (Listener listen : listener) {
                    listen.onSyncUpdate(folder.getFullName());
                }
            }

            store.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void merge(Listener... listener) {
        initCache();
        disableChangeListening();
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
                    ArrayList<Message> messagesToAdd = new ArrayList<Message>();
                    for (Note note : notes) {
                        Message[] messages = folder.getMessages();

                        event = getEvent(note.getIdentification().getUid());
                        if (event == Type.NEW) {
                            MimeMessage message = new MimeMessage(Session.getInstance(System.getProperties()));

                            message.addHeader("From", account.getUsername());
                            message.addHeader("To", account.getUsername());
                            message.addHeader("Date", new Date(note.getAuditInformation().getLastModificationDate().getTime()).toString());
                            message.addHeader("X-Kolab-Type", "application/x-vnd.kolab.note");
                            message.addHeader("X-Kolab-Mime-Version", "3.0");
                            message.addHeader("User-Agent", "kolabnotes-java");

                            message.setFrom(new InternetAddress(account.getUsername()));
                            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.getUsername()));
                            message.setSubject(note.getIdentification().getUid());
                            Multipart multipart = new MimeMultipart();

                            //Text art
                            MimeBodyPart textPart = new MimeBodyPart();
                            textPart.setText(KOLAB_TEXT, "UTF-8");
                            multipart.addBodyPart(textPart);
                            
                            setKolabXML(note, multipart);

                            message.setContent(multipart);
                            message.saveChanges();
                            messagesToAdd.add(message);
                        } else if (event == Type.UPDATE) {
                            Timestamp now = new Timestamp(System.currentTimeMillis());
                            String uid = note.getIdentification().getUid();

                            MimeMessage message = findMessage(uid, messages);

                            setKolabXML(note, (Multipart) message.getContent());
                            message.saveChanges();
                        } else if (event == Type.DELETE) {
                            Message message = findMessage(note.getIdentification().getUid(), messages);
                            if (message != null) {
                                Flags deleted = new Flags(Flags.Flag.DELETED);
                                folder.setFlags(new Message[]{message}, deleted, true);
                            }
                        }
                    }
                    folder.appendMessages(messagesToAdd.toArray(new Message[messagesToAdd.size()]));
                    folder.close(true);
                }
            }
            store.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            enableChangeListening();
        }
    }

    void setKolabXML(Note note, Multipart content) throws MessagingException {
        for (int i = 0; i < content.getCount(); i++) {
            BodyPart bodyPart = content.getBodyPart(i);
            if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                content.removeBodyPart(i);
            }
        }
        
        MimeBodyPart newContent = new MimeBodyPart();
        newContent.setFileName("kolab.xml");
        newContent.setDataHandler(new IMAPNoteDataHandler(note, "APPLICATION/VND.KOLAB+XML", parser));
        content.addBodyPart(newContent, 1);
    }

    MimeMessage findMessage(String uid, Message[] messages) throws Exception {
        for (Message m : messages) {
            if (m.getSubject() != null && m.getSubject().contains(uid)) {
                return (MimeMessage) m;
            }
        }
        return null;
    }

    @Override
    protected void initCache() {
        if (notesCache.isEmpty()) {
            refresh();
        }
    }
    
    @Override
    protected void addNotebook(String uid, Notebook notebook) {
        super.addNotebook(uid, notebook);
    }
    
    @Override
    protected void addNote(String uid, Note note) {
        super.addNote(uid, note);
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
                    InputStream inputStream = bodyPart.getInputStream();
                    Note note = parser.parseNote(inputStream);
                    inputStream.close();
                    notebook.addNote(note);
                    addNote(note.getIdentification().getUid(), note);
                }
            }
        }
    }
}
