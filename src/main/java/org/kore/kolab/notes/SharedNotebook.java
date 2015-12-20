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

/**
 * Represents a notebook which is shared from an another user
 * @author Konrad Renner
 */
public class SharedNotebook extends Notebook{
    
    private String shortName;
    private boolean noteCreationAllowed;
    private boolean noteModificationAllowed;
    
    public SharedNotebook(Identification identification, AuditInformation auditInformation, Classification classification, String summary) {
        super(identification, auditInformation, classification, summary);
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public boolean isNoteCreationAllowed() {
        return noteCreationAllowed;
    }

    public void setNoteCreationAllowed(boolean noteCreationAllowed) {
        this.noteCreationAllowed = noteCreationAllowed;
    }

    public boolean isNoteModificationAllowed() {
        return noteModificationAllowed;
    }

    public void setNoteModificationAllowed(boolean noteModificationAllowed) {
        this.noteModificationAllowed = noteModificationAllowed;
    }

    @Override
    public String toString() {
        return "SharedNotebook{" + "shortName=" + shortName + ", noteCreationAllowed=" + noteCreationAllowed + ", noteModificationAllowed=" + noteModificationAllowed + '}';
    }

    @Override
    public boolean isShared(){
        return true;
    }
    
    /**
     * If the notebook is shared from another user (no global share) the username is returned.
     * Otherwise null is returned
     * 
     * @return String
     */
    public String getUsername(){
        if(isGlobalShared()){
            return null;
        }
        
        String summary = getSummary();
        int indexOfFirstSlash = summary.indexOf("/");
        int indexOfSecondSlash = summary.indexOf("/", indexOfFirstSlash+1);
        return summary.substring(indexOfFirstSlash+1,indexOfSecondSlash);
    }
    
    /**
     * Returns true, if the notebook is a global share
     * 
     * @return boolean
     */
    public boolean isGlobalShared(){
        return getSummary().startsWith("Shared Folders");
    }
}
