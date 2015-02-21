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

import org.junit.Test;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.v3.KolabNotesParserV3;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepositoryTest {

    public ImapRepositoryTest() {
    }

    @Test
    public void testSomeMethod() {
        AccountInformation info = AccountInformation.createForHost("imap.kolabnow.com").username("").password("").build();
        ImapRepository imapRepository = new ImapRepository(new KolabNotesParserV3(), info, "Notes");
        System.out.println(imapRepository.getNotes().toString());
        System.out.println(imapRepository.getNotebooks().toString());
    }
}
