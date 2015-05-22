package org.kore.kolab.notes.v3;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.imap.RemoteTags;


public class KolabConfigurationParserV3Test {

    private KolabConfigurationParserV3 parser;

    @Before
    public void setUp() {
        parser = new KolabConfigurationParserV3();
    }

    @Test
    public void testParseNote() {
        InputStream inputStream = getClass().getResourceAsStream("kolab_configuration.xml");
        RemoteTags.TagDetails tag = parser.parse(inputStream);

        assertEquals("Android", tag.getTag().getName());
        assertNotNull(tag.getAuditInformation().getCreationDate());
        assertNotNull(tag.getAuditInformation().getLastModificationDate());
        assertNotNull(tag.getIdentification().getUid());
        assertEquals("kolabtags-provider", tag.getIdentification().getProductId());
        assertTrue(tag.getTag().getPriority() == 0);
        assertTrue(tag.getMembers().size() == 2);
        System.out.println(tag);
    }

    @Test
    public void testWriteNote() throws Exception {
        Identification identification = new Identification("599d595c-a715-4a6f-821b-c368e4cb70c2", "kolabtags-provider");
        AuditInformation audit = new AuditInformation(new Timestamp(System.currentTimeMillis()),
                new Timestamp(System.currentTimeMillis()));

        Tag tag = new Tag("Work");
        tag.setPriority(1);
        Set<String> members = new LinkedHashSet<String>();
        members.add("Note1");
        members.add("Note2");
        RemoteTags.TagDetails tagDetail = new RemoteTags.TagDetails(identification, audit, tag, members);

        parser.write(tagDetail, System.out);
    }

}
