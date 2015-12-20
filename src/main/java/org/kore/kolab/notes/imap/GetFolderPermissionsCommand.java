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

import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author Konrad Renner
 */
public class GetFolderPermissionsCommand implements IMAPFolder.ProtocolCommand {

    private final static String[] MODIFICATION_RIGHTS = {"e", "t"};
    private final static String[] CREATION_RIGHTS = {"i", "s", "w"};

    private final String folderName;
    private boolean isNoteCreationAllowed;
    private boolean isNoteModificationAllowed;

    public GetFolderPermissionsCommand(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
        //shared folders containing /, which are forbidden for "normal" folders
        Argument command = new Argument();
        setFolderName(command);

        Response[] response = imapp.command("MYRIGHTS", command);

        for (int i = 0; i < response.length; i++) {
            String rest = response[i].getRest();

            //there is a whitespace if the result is in this string (in the one loop run there will be e.g. the string "Completed" if everything is ok)
            String[] splitted = rest.split(" ");

            if (splitted.length > 1) {
                //Details look https://tools.ietf.org/html/rfc4314#page-10
                //The rights are always the last part
                String imapPermissions = splitted[splitted.length - 1];

                boolean creationPossible = true;
                for (String creationRight : CREATION_RIGHTS) {
                    if (!imapPermissions.contains(creationRight)) {
                        creationPossible = false;
                    }
                }

                if (creationPossible) {
                    this.isNoteCreationAllowed = true;

                    boolean modificationPossible = true;
                    for (String modificationRight : MODIFICATION_RIGHTS) {
                        if (!imapPermissions.contains(modificationRight)) {
                            modificationPossible = false;
                        }
                    }

                    this.isNoteModificationAllowed = modificationPossible;
                }
                break;
            }
        }
        return null;
    }

    void setFolderName(Argument command) {
        try {
            command.writeString(folderName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            command.writeString(folderName);
        }
    }

    public boolean isIsNoteCreationAllowed() {
        return isNoteCreationAllowed;
    }

    public boolean isIsNoteModificationAllowed() {
        return isNoteModificationAllowed;
    }

}
