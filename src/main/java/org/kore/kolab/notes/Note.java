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
import java.sql.Timestamp;
import java.util.LinkedHashSet;
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
    private final Set<String> categories;
    private Classification classification;
    private Attachment attachment;
    private String summary;
    private String description;
    private Color color;

    public Note(Identification identification,
            AuditInformation auditInformation,
            Classification classification,
            String summary) {
        super();
        this.identification = identification;
        this.auditInformation = auditInformation;
        this.classification = classification;
        this.summary = summary;
        this.categories = new LinkedHashSet<String>();
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


    public void addCategories(String... cats) {
        for (String cat : cats) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.NEW, "categories", null, cat);
            this.categories.add(cat);
        }
    }

    public void removeCategories(String... cats) {
        for (String cat : cats) {
            firePropertyChange(getIdentification().getUid(), EventListener.Type.DELETE, "categories", cat, null);
            this.categories.remove(cat);
        }
    }

    public Classification getClassification() {
        return classification;
    }

    public void setClassification(Classification classification) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "classification", this.classification, classification);
        this.classification = classification;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public void setAttachment(Attachment attachment) {
        firePropertyChange(getIdentification().getUid(), EventListener.Type.UPDATE, "attachment", this.attachment, attachment);
        this.attachment = attachment;
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

    public Set<String> getCategories() {
        return categories;
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
        return "Note [identification=" + identification
                + ", auditInformation="
                + auditInformation
                + ", categories="
                + categories
                + ", classification="
                + classification
                + ", attachment="
                + attachment
                + ", summary="
                + summary
                + ", description="
                + description
                + ", color="
                + color
                + "]";
    }


    public static enum Classification {

        PUBLIC,
        CONFIDENTIAL,
        PRIVATE;
    }

    public static class Identification
            implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String uid;
        private final String productId;

        public Identification(String uid, String productId) {
            if (uid == null || productId == null) {
                throw new IllegalArgumentException("given parameters must not be null");
            }

            this.uid = uid;
            this.productId = productId;
        }

        public String getUid() {
            return uid;
        }

        public String getProductId() {
            return productId;
        }

        @Override
        public String toString() {
            return "Identification [uid=" + uid + ", productId=" + productId + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((productId == null) ? 0 : productId.hashCode());
            result = prime * result + ((uid == null) ? 0 : uid.hashCode());
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
            Identification other = (Identification) obj;
            if (productId == null) {
                if (other.productId != null) {
                    return false;
                }
            } else if (!productId.equals(other.productId)) {
                return false;
            }
            if (uid == null) {
                if (other.uid != null) {
                    return false;
                }
            } else if (!uid.equals(other.uid)) {
                return false;
            }
            return true;
        }

    }

    public static class AuditInformation
            implements Serializable, Comparable<AuditInformation> {

        private static final long serialVersionUID = 1L;

        private final Timestamp creationDate;
        private Timestamp lastModificationDate;

        public AuditInformation(Timestamp creationDate, Timestamp lastModificationDate) {
            if (creationDate == null || lastModificationDate == null) {
                throw new IllegalArgumentException("given parameters must not be null");
            }
            this.creationDate = new Timestamp(creationDate.getTime());
            this.lastModificationDate = new Timestamp(lastModificationDate.getTime());
        }

        public Timestamp getCreationDate() {
            return new Timestamp(creationDate.getTime());
        }

        public Timestamp getLastModificationDate() {
            return new Timestamp(lastModificationDate.getTime());
        }

        public void setLastModificationDate(long millis) {
            this.lastModificationDate = new Timestamp(millis);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
            result = prime * result + ((lastModificationDate == null) ? 0 : lastModificationDate.hashCode());
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
            AuditInformation other = (AuditInformation) obj;
            if (creationDate == null) {
                if (other.creationDate != null) {
                    return false;
                }
            } else if (!creationDate.equals(other.creationDate)) {
                return false;
            }
            if (lastModificationDate == null) {
                if (other.lastModificationDate != null) {
                    return false;
                }
            } else if (!lastModificationDate.equals(other.lastModificationDate)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Audit [creationDate=" + creationDate + ", lastModificationDate=" + lastModificationDate + "]";
        }

        @Override
        public int compareTo(AuditInformation o) {
            int first = getLastModificationDate().compareTo(o.getLastModificationDate());
            return first == 0 ? getCreationDate().compareTo(o.getCreationDate()) : first;
        }

    }
}
