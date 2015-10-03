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
import java.util.UUID;

/**
 *
 * @author Konrad Renner
 */
public class Tag implements Serializable, Comparable<Tag> {

    private final Identification identification;
    private final AuditInformation auditInformation;
    private String name;
    private int priority;
    private Color color;

    public Tag(Identification identification, AuditInformation auditInformation) {
        this.identification = identification;
        this.auditInformation = auditInformation;
    }

    public static Tag createNewTag(String name) {
        Identification identification = new Identification(UUID.randomUUID().toString(), "kolabnotes-java");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        AuditInformation audit = new AuditInformation(now, now);

        Tag tag = new Tag(identification, audit);
        tag.setName(name);
        return tag;
    }

    public Identification getIdentification() {
        return identification;
    }

    public AuditInformation getAuditInformation() {
        return auditInformation;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }


    @Override
    public int compareTo(Tag o) {
        int compare = priority - o.getPriority();
        if (compare == 0) {
            compare = name.compareTo(o.getName());
        }
        return compare;
    }

    @Override
    public String toString() {
        return "Tag{" + "identification=" + identification + ", auditInformation=" + auditInformation + ", name=" + name + ", priority=" + priority + ", color=" + color + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + (this.name != null ? this.name.hashCode() : 0);
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
        final Tag other = (Tag) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

}
