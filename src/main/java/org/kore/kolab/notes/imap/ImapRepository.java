/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kore.kolab.notes.imap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.KolabNotesParser;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.RemoteNotesRepository;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepository implements RemoteNotesRepository {

    private final Map<String, Notebook> cache;
    private final KolabNotesParser parser;
    private final AccountInformation account;
    private final String rootfolder;

    public ImapRepository(KolabNotesParser parser, AccountInformation account, String rootFolder) {
        this.cache = new ConcurrentHashMap<String, Notebook>();
        this.parser = parser;
        this.account = account;
        this.rootfolder = rootFolder;
    }

    @Override
    public Note getNote(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<Note> getNotes() {
        try {
            initCache();
            return Collections.unmodifiableCollection(cache.values());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public Collection<Notebook> getNotebooks() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Notebook getNotebook(String uid) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    @Override
    public boolean deleteNote(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean storeNote(Note note) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void refresh() {
        cache.clear();
        try {
            initCache();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void merge() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


    void initCache() throws Exception {
        Properties props = new Properties();

        Session session = Session.getInstance(props, null);

        Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imaps");

        store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

        Folder rFolder = store.getFolder(rootfolder);
        initNotesFromFolder(rFolder);

        Folder[] allFolders = rFolder.list("*");

        for (Folder folder : allFolders) {
            initNotesFromFolder(folder);
        }

        store.close();
    }

    void initNotesFromFolder(Folder folder) throws MessagingException, IOException {
        folder.open(Folder.READ_ONLY);
        Message[] messages = folder.getMessages();

        for (Message m : messages) {
            Multipart content = (Multipart) m.getContent();
            for (int i = 0; i < content.getCount(); i++) {
                BodyPart bodyPart = content.getBodyPart(i);
                if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                    Note note = parser.parseNote(bodyPart.getInputStream());
                    cache.put(note.getIdentification().getUid(), note);
                }
            }
        }
    }
}
