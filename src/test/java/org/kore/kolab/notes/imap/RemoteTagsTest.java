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

import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.v3.KolabConfigurationParserV3;

/**
 *
 * @author Konrad Renner
 */
public class RemoteTagsTest {

    private RemoteTags remoteTags;

    @Before
    public void setUp() {
        AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
        remoteTags = new RemoteTags(new KolabConfigurationParserV3(), info, "Notes");
    }

    @Ignore
    @Test
    public void testGetTags() {
        //may fail, if ids not present on test server
        Set<RemoteTags.TagDetails> tags = remoteTags.getTags();

        for (RemoteTags.TagDetails tag : tags) {
            System.out.println(tag.toString());
        }
    }

    @Ignore
    @Test
    public void testSetTags() {
        //may fail, if ids not present on test server
        remoteTags.init(null);

        remoteTags.removeTags("d4b7853b-63ab-4a28-9e51-94414f817a94");
        remoteTags.attachTags("d4b7853b-63ab-4a28-9e51-94414f817a94", Tag.createNewTag("Android"));

        remoteTags.removeTags("36e36957-2b32-4f47-ae63-469118764373");
        remoteTags.attachTags("36e36957-2b32-4f47-ae63-469118764373", Tag.createNewTag("Linux"), Tag.createNewTag("Android"));

        remoteTags.merge();
    }
}
