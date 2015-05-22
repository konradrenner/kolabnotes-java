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
import java.util.Arrays;

/**
 *
 * @author Konrad Renner
 */
public class SetConfigurationCommand implements IMAPFolder.ProtocolCommand {

    private final String folderName;

    public SetConfigurationCommand(String folderName) {
        this.folderName = folderName;
    }

    @Override
    public Object doCommand(IMAPProtocol imapp) throws ProtocolException {
        Argument command = new Argument();
        Argument listArguments = new Argument();

        command.writeString(folderName);

        command.writeNString("/vendor/kolab/folder-type");

        listArguments.writeNString("value.shared");
        listArguments.writeNString("configuration");

        listArguments.writeNString("value.priv");
        listArguments.writeNString("configuration.default");

        command.writeArgument(listArguments);

        Response[] response = imapp.command("SETANNOTATION", command);

        if (response.length == 1 && response[0].isOK()) {
            return null;
        }

        throw new ProtocolException("Unable to set folder-type." + Arrays.toString(response));
    }

}
