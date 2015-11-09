/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.kore.kolab.notes.imap;

import com.sun.mail.imap.IMAPFolder;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import korex.activation.DataHandler;
import korex.mail.BodyPart;
import korex.mail.FetchProfile;
import korex.mail.Flags;
import korex.mail.Folder;
import static korex.mail.Folder.READ_ONLY;
import static korex.mail.Folder.READ_WRITE;
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
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.KolabParser;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.RemoteNotesRepository;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.local.LocalNotesRepository;

/**
 *
 * @author Konrad Renner
 */
public class ImapNotesRepository extends LocalNotesRepository implements RemoteNotesRepository {

    final static String NOT_LOADED = "NOT_LOADED";
    final static String KOLAB_TEXT = "This is a Kolab Groupware object.\n"
            + "To view this object you will need a Kolab Groupware Client.\n"
            + "For a list of Kolab Groupware Clients please visit:\n"
            + "http://www.kolab.org/get-kolab";

    private final AccountInformation account;
    private final KolabParser configurationParser;
    private RemoteTags remoteTags;

    public ImapNotesRepository(KolabParser parser, AccountInformation account, String rootFolder, KolabParser configurationParser) {
        super(parser, rootFolder);
        this.account = account;
        this.configurationParser = configurationParser;
    }

    public RemoteTags getRemoteTags() {
        return remoteTags;
    }

    @Override
    public void merge(Map<String, Type> eventTypes, Listener... listener) {
        eventCache.putAll(eventTypes);
        merge(listener);
    }

    @Override
    public void refresh(Listener... listener) {
        refresh(null, listener);
    }

    @Override
    public void refresh(Date modificationDate, Listener... listener) {
        notesCache.clear();
        notebookCache.clear();
        try {
            Properties props = new Properties();

            //TODO refactor with an ssl trust store
            if (account.isSSLEnabled()) {
                props.put("mail.imaps.ssl.trust", "*");
                //try to use starttls
                props.put("mail.imap.starttls.enable", "true");
            }

            Session session = Session.getInstance(props, null);

            Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imap");

            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            remoteTags = new RemoteTags(configurationParser, account);
            remoteTags.init(store);

            Folder rFolder = store.getFolder(rootfolder);
            FetchProfile fetchProfile = new FetchProfile();

            rFolder.open(READ_ONLY);
            if (account.isFolderAnnotationEnabled()) {
                initNotesFromFolderWithAnnotationCheck(rFolder, fetchProfile, modificationDate);
            } else {
                initNotesFromFolder(rFolder, fetchProfile, modificationDate,false);
            }

            Folder[] allFolders = rFolder.list("*");

            for (Folder folder : allFolders) {
                folder.open(READ_ONLY);
                if (account.isFolderAnnotationEnabled()) {
                    initNotesFromFolderWithAnnotationCheck(folder, fetchProfile, modificationDate);
                } else {
                    initNotesFromFolder(folder, fetchProfile, modificationDate,false);
                }
                
                for (Listener listen : listener) {
                    listen.onSyncUpdate(folder.getFullName());
                }
                //folder.close(false);
            }
            
            initSharedFolders(store, fetchProfile, modificationDate, account.isFolderAnnotationEnabled(), listener);

            //rFolder.close(false);
            store.close();

            eventCache.clear();
            deletedNotebookCache.clear();
            deletedNotesCache.clear();
        } catch (Exception e) {
            System.out.println(e);
            throw new IllegalStateException(e);
        }
    }
    
    void initSharedFolders(Store store, FetchProfile fetchProfile, Date modificationDate, boolean folderAnnotationEnabled, Listener... listener) throws MessagingException, IOException{
        if(!folderAnnotationEnabled){
            //Just Kolab servers are enabled for this feature
            return;
        }
        
        Folder defaultFolder = store.getDefaultFolder();
        
        for(Folder folder : defaultFolder.list("*")){
            folder.open(READ_ONLY);
            
            if (folder instanceof IMAPFolder) {
                GetSharedFolderCommand metadataCommand = new GetSharedFolderCommand(folder.getFullName());
                ((IMAPFolder) folder).doCommand(metadataCommand);

                //Just handle folders which contain notes
                if (metadataCommand.isSharedNotesFolder()) {
                    initNotesFromFolder(folder, fetchProfile, modificationDate, true);

                    for (Listener listen : listener) {
                        listen.onSyncUpdate(folder.getFullName());
                    }
                }
            }
        }
    }

    @Override
    public void merge(Listener... listener) {
        initCache();
        disableChangeListening();
        try {
            Properties props = new Properties();

            //TODO refactor with an ssl trust store
            if (account.isSSLEnabled()) {
                props.put("mail.imaps.ssl.trust", "*");
            }

            Session session = Session.getInstance(props, null);

            Store store = account.isSSLEnabled() ? session.getStore("imaps") : session.getStore("imap");

            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

            if (remoteTags == null) {
                remoteTags = new RemoteTags(configurationParser, account);
            }
            remoteTags.init(store);

            //Actual there are no notebooks in notebooks supported
            ArrayList<Notebook> notebooks = new ArrayList<Notebook>(getNotebooks());
            //Deleted notebooks must be merged with the server too (delete from server)
            notebooks.addAll(deletedNotebookCache.values());

            //Get the root folder, so that new folders can be created under it
            IMAPFolder rootFolder = (IMAPFolder) store.getFolder(rootfolder);

            Flags deleted = new Flags(Flags.Flag.DELETED);


            for (Notebook book : notebooks) {
                IMAPFolder folder;
                if (rootfolder.equals(book.getSummary())) {
                    folder = rootFolder;
                } else {
                    folder = (IMAPFolder) rootFolder.getFolder(book.getSummary());
                }

                Type event = getEvent(book.getIdentification().getUid());
                if (event != null) {
                    if (event == Type.DELETE) {
                        folder.delete(true);
                        continue;
                    } else if (event == Type.NEW || event == Type.UPDATE) {
                        if (event == Type.NEW) {
                            folder.create(Folder.HOLDS_MESSAGES);
                        } else {
                            //TODO
                            folder.renameTo(folder);
                        }

                        if (account.isFolderAnnotationEnabled()) {
                            folder.doCommand(new SetMetadataCommand(folder.getFullName()));
                        }
                    }
                }

                //if the folder does not exist, do nothing
                if (folder.exists()) {
                    if (!folder.isOpen()) {
                        folder.open(READ_WRITE);
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

                        //IMAPMessages are readonly, so in case of update, first delete the old note, then create a new one                        
                        if (event == Type.UPDATE) {
                            String uid = note.getIdentification().getUid();

                            MimeMessage message = findMessage(uid, messages);

                            if (message != null) {
                                folder.setFlags(new Message[]{message}, deleted, true);
                            }
                        }

                        if (event == Type.NEW || event == Type.UPDATE) {

                            messagesToAdd.add(createMessage(account,
                                    note.getIdentification(),
                                    note.getAuditInformation(),
                                    new IMAPKolabDataHandler(note, "APPLICATION/VND.KOLAB+XML", parser),
                                    "application/x-vnd.kolab.note"));
                            
                            String uid = note.getIdentification().getUid();
                            remoteTags.removeTags(uid);
                            remoteTags.attachTags(uid, note.getCategories().toArray(new Tag[note.getCategories().size()]));
                        } else if (event == Type.DELETE) {
                            Message message = findMessage(note.getIdentification().getUid(), messages);
                            if (message != null) {
                                folder.setFlags(new Message[]{message}, deleted, true);
                            }
                            
                            remoteTags.removeTags(note.getIdentification().getUid());
                        }
                    }
                    folder.addMessages(messagesToAdd.toArray(new Message[messagesToAdd.size()]));
                    folder.close(true);
                }
            }

            remoteTags.merge();

            store.close();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            enableChangeListening();
        }
    }

    @Override
    public void fillUnloadedNote(Note note) {
        disableChangeListening();

        Note unloaded = notesCache.get(note.getIdentification().getUid());

        if (unloaded != null && NOT_LOADED.equals(unloaded.getSummary())) {
            unloaded.setClassification(note.getClassification());
            unloaded.setColor(note.getColor());
            unloaded.setDescription(note.getDescription());
            unloaded.setSummary(note.getSummary());
            unloaded.addCategories(note.getCategories().toArray(new Tag[note.getCategories().size()]));
            unloaded.getAuditInformation().setLastModificationDate(note.getAuditInformation().getLastModificationDate().getTime());
            unloaded.getAuditInformation().setCreationDate(note.getAuditInformation().getCreationDate().getTime());
        }

        enableChangeListening();
    }


    static Message createMessage(AccountInformation account, Identification ident, AuditInformation audit, DataHandler handler, String type) throws MessagingException {
        MimeMessage message = new MimeMessage(Session.getInstance(System.getProperties()));

        message.setFrom(new InternetAddress(account.getUsername()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.getUsername()));
        message.setSentDate(audit.getLastModificationDate());
        message.setSubject(ident.getUid(), "UTF-8");

        message.setHeader("X-Kolab-Type", type);
        message.setHeader("X-Kolab-Mime-Version", "3.0");
        message.setHeader("User-Agent", "kolabnotes-java");

        Multipart multipart = new MimeMultipart();

        //Text art
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(KOLAB_TEXT, "UTF-8");
        multipart.addBodyPart(textPart);

        setKolabXML(multipart, handler);

        message.setContent(multipart);
        message.saveChanges();
        //Maybe a problem, details: https://issues.kolab.org/show_bug.cgi?id=5109
        //message.setFlag(Flags.Flag.SEEN, true);

        return message;
    }

    static void setKolabXML(Multipart content, DataHandler handler) throws MessagingException {
        if (content == null) {
            return;
        }
        for (int i = 0; i < content.getCount(); i++) {
            BodyPart bodyPart = content.getBodyPart(i);
            if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                content.removeBodyPart(i);
            }
        }
        
        MimeBodyPart newContent = new MimeBodyPart();
        newContent.setFileName("kolab.xml");
        newContent.setDataHandler(handler);
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
    
    void initNotesFromFolderWithAnnotationCheck(Folder folder, FetchProfile fetchProfile, Date parseDate) throws MessagingException, IOException {
        if (folder instanceof IMAPFolder) {
            GetMetadataCommand metadataCommand = new GetMetadataCommand(folder.getFullName());
            ((IMAPFolder) folder).doCommand(metadataCommand);

            //Just handle folders which contain notes
            if (!metadataCommand.isNotesFolder()) {
                return;
            }
        }
        
        initNotesFromFolder(folder, fetchProfile, parseDate, false);
    }

    @Override
    public boolean noteCompletelyLoaded(Note note) {
        return !NOT_LOADED.equals(note.getSummary());
    }


    /**
     * Inits notes from a folder. If the parseDate is given, notes will just be
     * completly loaded where the sent date is after the parseDate, otherwise,
     * just empty notes will be created.
     *
     * @param folder
     * @param fetchProfile
     * @param parseDate
     * @param sharedFolder
     * @throws MessagingException
     * @throws IOException
     */
    void initNotesFromFolder(Folder folder, FetchProfile fetchProfile, Date parseDate, boolean sharedFolder) throws MessagingException, IOException {
        Message[] messages = folder.getMessages();

        fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(FetchProfile.Item.ENVELOPE);
        folder.fetch(messages, fetchProfile);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        Identification id = new Identification(Long.toString(System.currentTimeMillis()), "kolabnotes-java");
        AuditInformation audit = new AuditInformation(now, now);

        Notebook notebook;
        if(sharedFolder){
            SharedNotebook nb = new SharedNotebook(id, audit, Note.Classification.PUBLIC, folder.getName());
            int lastIndexOfSlash = nb.getSummary().lastIndexOf("/");
            nb.setShortName(nb.getSummary().substring(lastIndexOfSlash+1));
                    
            notebook = nb;
        }else{
           notebook = new Notebook(id, audit, Note.Classification.PUBLIC, folder.getName()); 
        }
        addNotebook(notebook.getIdentification().getUid(), notebook);

        for (Message m : messages) {
            Date sentDate = m.getSentDate();
            if (parseDate != null && parseDate.after(sentDate)) {
                Timestamp tst = new Timestamp(sentDate.getTime());
                Identification noteLoadedId = new Identification(m.getSubject(), "kolabnotes-java");
                AuditInformation notLoadedAudit = new AuditInformation(tst, tst);
                Note note = new Note(noteLoadedId, notLoadedAudit, Note.Classification.PUBLIC, NOT_LOADED);

                notebook.addNote(note);
                addNote(note.getIdentification().getUid(), note);
            } else {

            Multipart content = (Multipart) m.getContent();
                for (int i = 0; i < content.getCount(); i++) {
                    BodyPart bodyPart = content.getBodyPart(i);
                    if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                        InputStream inputStream = bodyPart.getInputStream();
                        Note note = (Note) parser.parse(inputStream);
                        inputStream.close();
                        notebook.addNote(note);
                        addNote(note.getIdentification().getUid(), note);

                        Set<RemoteTags.TagDetails> tagsFromNote = this.remoteTags.getTagsFromNote(note.getIdentification().getUid());
                        for (RemoteTags.TagDetails tag : tagsFromNote) {
                            note.addCategories(tag.getTag());
                        }
                    }
                }
            }
        }
    }
}
