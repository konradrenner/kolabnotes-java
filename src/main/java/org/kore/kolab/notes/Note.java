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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.kore.kolab.notes.event.AbstractEventSupport;
import org.kore.kolab.notes.event.EventListener;


/**
 * This class represents a kolab note (a 'standalone' note) as specified here:
 * https://wiki.kolab.org/User:Mollekopf/Drafts/KEP:17#Note_3
 * 
 * @author Konrad Renner
 * 
 */
public class Note extends AbstractEventSupport implements Serializable, Comparable<Note> {

    private static final long serialVersionUID = 1L;

    private final Identification identification;
    private final AuditInformation auditInformation;
    private final Set<Tag> categories;
    private Classification classification;
    private String summary;
    private String description;
    private Color color;
    private final Map<String, Attachment> attachments;

    public Note(Identification identification,
            AuditInformation auditInformation,
            Classification classification,
            String summary) {
        super();
        this.identification = identification;
        this.auditInformation = auditInformation;
        this.classification = classification;
        this.summary = summary;
        this.categories = new LinkedHashSet<Tag>();
        this.attachments = new LinkedHashMap<String, Attachment>();
    }

    @Override
    public int compareTo(Note o) {
        if (this.equals(o)) {
            return 0;
        }

        int ergebnis = o.getAuditInformation().getLastModificationDate().compareTo(getAuditInformation().getLastModificationDate());

        if (ergebnis == 0) {
            ergebnis = getSummary().compareTo(o.getSummary());
        }
        return ergebnis;
    }


    public void addCategories(Tag... cats) {
        for (Tag cat : cats) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.NEW, "categories", null, cat);
            this.categories.add(cat);
        }
    }

    public void removeCategories(Tag... cats) {
        for (Tag cat : cats) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.DELETE, "categories", cat, null);
            this.categories.remove(cat);
        }
    }

    public void addAttachments(Attachment... atts) {
        for (Attachment att : atts) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.NEW, "attachments", null, att);
            this.attachments.put(att.getId(), att);
        }
    }

    public void removeAttachments(String... attId) {
        for (String att : attId) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.DELETE, "attachments", att, null);
            this.attachments.remove(att);
        }
    }

    public void removeAttachments(Attachment... atts) {
        for (Attachment att : atts) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.DELETE, "attachments", att.getId(), null);
            this.attachments.remove(att.getId());
        }
    }

    public Attachment getAttachment(String id) {
        return this.attachments.get(id);
    }

    public Collection<Attachment> getAttachments() {
        return Collections.unmodifiableCollection(this.attachments.values());
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "classification", this.classification, classification);
        this.classification = classification;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "summary", this.summary, summary);
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "description", this.description, description);
        this.description = description;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "color", this.color, color);
        this.color = color;
    }

    public Identification getIdentification() {
        return identification;
    }

    public AuditInformation getAuditInformation() {
        return auditInformation;
    }

    public Set<Tag> getCategories() {
        return Collections.unmodifiableSet(categories);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identification == null) ? 0 : identification.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Note other = (Note) obj;
        if (identification == null) {
            if (other.identification != null) {
                return false;
            }
        } else if (!identification.equals(other.identification)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Note{" + "identification=" + identification + ", auditInformation=" + auditInformation + ", categories=" + categories + ", classification=" + classification + ", summary=" + summary + ", description=" + description + ", color=" + color + ", attachments=" + attachments + '}';
    }



    public static enum Classification {

        PUBLIC,
        CONFIDENTIAL,
        PRIVATE;
    }


}
