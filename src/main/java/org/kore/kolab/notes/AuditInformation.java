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

/**
 *
 * @author Konrad Renner
 */
public class AuditInformation implements Serializable, Comparable<AuditInformation> {
    private static final long serialVersionUID = 1L;
    private Timestamp creationDate;
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

    public void setCreationDate(long millis) {
        this.creationDate = new Timestamp(millis);
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
