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
package org.kore.kolab.notes.imap;

import com.sun.mail.imap.IMAPFolder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import korex.mail.BodyPart;
import korex.mail.FetchProfile;
import korex.mail.Flags;
import korex.mail.Folder;
import korex.mail.Message;
import korex.mail.MessagingException;
import korex.mail.Multipart;
import korex.mail.Store;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.KolabParser;
import org.kore.kolab.notes.Tag;

/**
 *
 * @author Konrad Renner
 */
public class RemoteTags {

    public static final String TYPE = "relation";
    public static final String RELATION_TYPE = "tag";

    private final AccountInformation account;
    private Set<TagDetails> remoteTags;
    private final Map<String, Set<TagDetails>> tagsPerNote;
    private final Map<String, TagDetails> tagPerTagname;
    private final KolabParser parser;
    
    private final Set<String> tagsForDeletion;

    public RemoteTags(KolabParser parser, AccountInformation login) {
        this.account = login;
        this.tagsPerNote = new HashMap<String, Set<TagDetails>>();
        this.tagPerTagname = new HashMap<String, TagDetails>();
        this.parser = parser;
        this.tagsForDeletion = new HashSet<String>();
    }

    /**
     * Gets all tags from the remote server
     *
     * @return Set<TagDetails>
     */
    public Set<TagDetails> getTags() {
        init(null);
        return Collections.unmodifiableSet(remoteTags);
    }
    
    public void applyLocalChanges(Tag... tags) {
        for (Tag tag : tags) {
            for (TagDetails detail : remoteTags) {
                if (detail.getIdentification().getUid().equals(tag.getIdentification().getUid())) {
                    detail.getTag().setColor(tag.getColor());
                    detail.getTag().setName(tag.getName());
                    detail.getTag().setPriority(tag.getPriority());
                    break;
                }
            }
        }
    }
    
    public void deleteTags(Identification... ident) {
        for (Identification i : ident) {
            tagsForDeletion.add(i.getUid());
        }
    }

    /**
     * Gets all tags from the note with the given uid
     *
     * @param uid
     * @return Set<TagDetails>
     */
    public Set<TagDetails> getTagsFromNote(String uid) {
        init(null);
        return tagsPerNote.get(uid) == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(tagsPerNote.get(uid));
    }

    /**
     * Gets a tag detail
     *
     * @param name
     * @return Set<TagDetails>
     */
    public TagDetails getTag(String name) {
        init(null);
        return tagPerTagname.get(name);
    }

    /**
     * Attaches the given tags to a note with the given uid
     *
     * @param uid
     * @param tags
     */
    public void attachTags(String uid, Tag... tags) {
        for (Tag tag : tags) {
            TagDetails actDetail = this.tagPerTagname.get(tag.getName());
            if (actDetail == null) {
                //create a new tag
                LinkedHashSet<String> member = new LinkedHashSet<String>();
                actDetail = new TagDetails(tag.getIdentification(), tag.getAuditInformation(), tag, member);

                this.tagPerTagname.put(tag.getName(), actDetail);
                if (this.remoteTags == null) {
                    this.remoteTags = new LinkedHashSet<TagDetails>();
                }
                this.remoteTags.add(actDetail);
            }
            actDetail.addMember(uid);

            Set<TagDetails> perNote = this.tagsPerNote.get(uid);
            if (perNote == null) {
                perNote = new LinkedHashSet<TagDetails>();
                this.tagsPerNote.put(uid, perNote);
            }
            perNote.add(actDetail);
        }
    }

    /**
     * Removes all tags from the note with the given uid
     *
     * @param uid
     */
    public void removeTags(String uid) {
        Set<TagDetails> tags = getTagsFromNote(uid);

        for (TagDetails tag : tags) {
            tag.removeMember(uid);
        }
        this.tagsPerNote.put(uid, new LinkedHashSet<TagDetails>());
    }

    Store connect(Store store) throws MessagingException {
        if (store == null) {
            store = ImapNotesRepository.openConnection(account);
        }

        if (!store.isConnected()) {
            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
        }
        return store;
    }

    void init(Store store) {
        if (this.remoteTags != null) {
            return;
        }

        this.remoteTags = new LinkedHashSet<TagDetails>();

        try {
            Store lstore = connect(store);

            Folder rFolder = lstore.getDefaultFolder();
            Folder configFolder = searchConfigFolder(rFolder);

            if (configFolder != null) {
                configFolder.open(Folder.READ_ONLY);

                Message[] messages = configFolder.getMessages();

                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
                fetchProfile.add(FetchProfile.Item.FLAGS);
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                configFolder.fetch(messages, fetchProfile);

                for (Message message : messages) {
                    String[] header = message.getHeader("X-Kolab-Type");
                    if (Arrays.toString(header).contains("application/x-vnd.kolab.configuration.relation")) {
                        TagDetails tag = getFromMessage(message);

                        if (tag != null) {
                            this.remoteTags.add(tag);
                            this.tagPerTagname.put(tag.getTag().getName(), tag);
                            addToNotesMap(tag);
                        }
                    }
                }
                
                configFolder.close(false);
            }

            if (store == null) {
                lstore.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

    }

    TagDetails getFromMessage(Message message) throws IOException, MessagingException {
        Multipart content = (Multipart) message.getContent();
        for (int i = 0; i < content.getCount(); i++) {
            BodyPart bodyPart = content.getBodyPart(i);
            if (bodyPart.getContentType().startsWith("APPLICATION/VND.KOLAB+XML")) {
                InputStream inputStream = bodyPart.getInputStream();
                TagDetails tag = (TagDetails) parser.parse(inputStream);
                inputStream.close();
                return tag;
            }
        }
        return null;
    }

    public void merge() {
        merge(null);
    }

    void merge(Store store) {
        init(store);

        try {
            Store lstore = connect(store);

            Folder rFolder = lstore.getDefaultFolder();
            Folder configFolder = searchConfigFolder(rFolder);

            if (configFolder == null) {
                configFolder = rFolder.getFolder("Configuration");
                configFolder.create(Folder.HOLDS_MESSAGES);

                if (account.isFolderAnnotationEnabled()) {
                    ((IMAPFolder) configFolder).doCommand(new SetConfigurationCommand(configFolder.getFullName()));
                }
            }
            
            if (!configFolder.isOpen()) {
                configFolder.open(Folder.READ_WRITE);
            }

            Message[] serverTags = configFolder.getMessages();

            Flags deleted = new Flags(Flags.Flag.DELETED);

            ArrayList<Message> messagesToAdd = new ArrayList<Message>();
            boolean createMessage = true;
            for (TagDetails detail : remoteTags) {
                Message serverTag = searchForRemoteTag(detail.getIdentification().getUid(), serverTags);

                if (serverTag != null) {
                    if (tagsForDeletion.contains(detail.getIdentification().getUid())) {
                        configFolder.setFlags(new Message[]{serverTag}, deleted, true);
                        createMessage = false;
                    } else {

                        TagDetails fromMessage = getFromMessage(serverTag);

                        //If the remote and the local TagDetails contain the same entries, nothing must be done
                        if (noChange(detail, fromMessage)) {
                            createMessage = false;
                        } else {
                            configFolder.setFlags(new Message[]{serverTag}, deleted, true);
                        }
                    }
                }

                if (createMessage) {
                    Message tagMessage = ImapNotesRepository.createMessage(account,
                            detail.getIdentification(),
                            detail.getAuditInformation(),
                            new IMAPKolabDataHandler(detail, "APPLICATION/VND.KOLAB+XML", parser),
                            "application/x-vnd.kolab.configuration.relation");

                    messagesToAdd.add(tagMessage);
                }
                createMessage = true;
            }

            ((IMAPFolder) configFolder).addMessages(messagesToAdd.toArray(new Message[messagesToAdd.size()]));
            configFolder.close(true);
            if (store == null) {
                lstore.close();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    boolean noChange(TagDetails local, TagDetails remote) {
        boolean noChange = remote.getMembers().equals(local.getMembers()) && remote.getTag().getPriority() == local.getTag().getPriority() && remote.getTag().getName().equals(local.getTag().getName());

        if (!noChange) {
            return false;
        }

        if (remote.getTag().getColor() != null) {
            noChange = remote.getTag().getColor().equals(local.getTag().getColor());
        } else if (local.getTag().getColor() != null) {
            noChange = false;
        }

        return noChange;
    }

    Message searchForRemoteTag(String uid, Message[] messages) throws MessagingException {
        for (Message m : messages) {
            if (uid.equals(m.getSubject())) {
                return m;
            }
        }
        return null;
    }
    
    void addToNotesMap(TagDetails tag) {
        for (String note : tag.getMembers()) {
            Set<TagDetails> tagsOfNote = this.tagsPerNote.get(note);
            
            if (tagsOfNote == null) {
                tagsOfNote = new LinkedHashSet<TagDetails>();
                this.tagsPerNote.put(note, tagsOfNote);
            }
            
            tagsOfNote.add(tag);
        }
    }

    Folder searchConfigFolder(Folder rFolder) throws MessagingException {
        for (Folder folder : rFolder.list("*")) {
            if (isConfigurationFolder(folder)) {
                return folder;
            }
        }
        return null;
    }

    boolean isConfigurationFolder(Folder folder) throws MessagingException {
        if (account.isFolderAnnotationEnabled()) {
            GetConfigurationCommand metadataCommand = new GetConfigurationCommand(folder.getFullName());
            ((IMAPFolder) folder).doCommand(metadataCommand);
            return metadataCommand.isConfigurationFolder();
        } else {
            return "Configuration".equalsIgnoreCase(folder.getName());
        }
    }

    public static class TagDetails {

        private final Tag tag;
        private final Set<String> members;
        private final AuditInformation auditInformation;
        private final Identification identification;

        public TagDetails(Identification identification, AuditInformation auditInformation, Tag tag, Set<String> members) {
            this.tag = tag;
            this.members = new LinkedHashSet<String>(members);
            this.auditInformation = auditInformation;
            this.identification = identification;
        }

        public Tag getTag() {
            return tag;
        }

        public Set<String> getMembers() {
            return Collections.unmodifiableSet(members);
        }

        public boolean containsMember(String uid) {
            return members.contains(uid);
        }

        public void removeMember(String uid) {
            members.remove(uid);
        }

        public void addMember(String uid) {
            members.add(uid);
        }

        public AuditInformation getAuditInformation() {
            return auditInformation;
        }

        public Identification getIdentification() {
            return identification;
        }

        @Override
        public String toString() {
            return "TagDetails{" + "tag=" + tag + ", members=" + members + ", auditInformation=" + auditInformation + ", identification=" + identification + '}';
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + (this.tag != null ? this.tag.hashCode() : 0);
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
            final TagDetails other = (TagDetails) obj;
            if (this.tag != other.tag && (this.tag == null || !this.tag.equals(other.tag))) {
                return false;
            }
            return true;
        }

    }
}
