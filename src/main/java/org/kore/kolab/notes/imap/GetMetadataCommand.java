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

/**
 *
 * @author Konrad Renner
 */
public class GetMetadataCommand implements IMAPFolder.ProtocolCommand {

    private final String folderName;
    private boolean isNotesFolder;

    public GetMetadataCommand(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
        Argument command = new Argument();
        //command.writeArgument(listOptions);
        command.writeString(folderName);

        command.writeNString("/vendor/kolab/folder-type");
        command.writeString("*");

        Response[] response = imapp.command("GETANNOTATION", command);

        for (int i = 0; i < response.length; i++) {
            String rest = response[i].getRest();

            if (rest.contains("note")) {
                isNotesFolder = true;
            }
        }

        return null;
    }

    public boolean isNotesFolder() {
        return isNotesFolder;
    }
}
