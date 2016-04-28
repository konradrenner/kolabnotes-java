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

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.kore.kolab.notes.AccountInformation;
import org.kore.kolab.notes.AuditInformation;
import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.SharedNotebook;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.event.EventListener;
import org.kore.kolab.notes.v3.KolabConfigurationParserV3;
import org.kore.kolab.notes.v3.KolabNotesParserV3;

/**
 *
 * @author Konrad Renner
 */
public class ImapRepositoryTest {

    private ImapNotesRepository imapRepository;

    @Before
    public void setUp() {
        AccountInformation info = AccountInformation.createForHost("imap.kolabnow.com").username("konrad.renner@kolabnow.com").password("ko1601re.ko").enableSharedFolders().build();
        imapRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, "Testbuch", new KolabConfigurationParserV3());

        createTestdata();
    }

    @Test
    public void testGetNote() {
        Note note = imapRepository.getNote("bookOnenoteOne");

        assertEquals("Note one", note.getSummary());
    }

    @Test
    public void testGetNotes() {
        Collection<Note> notes = imapRepository.getNotes();

        assertTrue(notes.size() == 3);
    }

    @Test
    public void testGetNotebooks() {
        Collection<Notebook> notebooks = imapRepository.getNotebooks();

        assertTrue(notebooks.size() == 4);
    }

    @Test
    public void testGetNotebook() {
        Notebook notebook = imapRepository.getNotebook("Notes");

        assertEquals("Notes", notebook.getSummary());
    }

    @Test
    public void testDeleteNotebook() {
        boolean deleted = imapRepository.deleteNotebook("Notes");

        assertTrue(deleted);
        assertNull(imapRepository.getNotebook("Notes"));
        assertEquals(EventListener.Type.DELETE, imapRepository.getEvent("Notes"));
    }

    @Test
    public void testCreateNotebook() {
        Notebook createNotebook = imapRepository.createNotebook("NewBookUID", "Cool New Book");
        
        assertEquals("Cool New Book", createNotebook.getSummary());
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewBookUID"));
    }
    
    @Test
    public void testCreateNote() {
        Note createNote = imapRepository.createNotebook("NewBookUID", "Cool New Book").createNote("NewNoteUID", "Summary");
        createNote.addCategories(Tag.createNewTag("Work"));

        assertEquals("Summary", createNote.getSummary());
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewBookUID"));
        assertEquals(createNote.getCategories().iterator().next(), Tag.createNewTag("Work"));
        assertEquals(EventListener.Type.NEW, imapRepository.getEvent("NewNoteUID"));
		assertEquals(createNote, imapRepository.getNote(createNote.getIdentification().getUid()));
    }

    @Test
    public void testUpdateNote() {
        Note note = imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne");
        Timestamp lastModificationDate = note.getAuditInformation().getLastModificationDate();
	note.setSummary("Hallo");

	assertEquals("Hallo", imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne").getSummary());
        assertEquals(EventListener.Type.UPDATE, imapRepository.getEvent("bookOnenoteOne"));
        assertTrue(lastModificationDate.before(note.getAuditInformation().getLastModificationDate()));
    }

    @Test
    public void testUpdateNoteLoadedRepository() {
	Note note = imapRepository.getNote("bookOnenoteOne");
	note.setSummary("Hallo");

	assertEquals("Hallo", imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne").getSummary());
	assertEquals(EventListener.Type.UPDATE, imapRepository.getEvent("bookOnenoteOne"));
    }

    @Test
    public void testDeleteNote() {
	imapRepository.getNotebook("bookOne").deleteNote("bookOnenoteOne");

	assertNull(imapRepository.getNotebook("bookOne").getNote("bookOnenoteOne"));
	assertEquals(EventListener.Type.DELETE, imapRepository.getEvent("bookOnenoteOne"));
    }

    @Test
    public void testFillUnloadedNote() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.YEAR, 2015);
        cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Timestamp now = new Timestamp(cal.getTimeInMillis());
        Identification ident = new Identification("bookOne", "kolabnotes-java");
        AuditInformation audit = new AuditInformation(now, now);

        Note note = new Note(ident, audit, Note.Classification.PUBLIC, ImapNotesRepository.NOT_LOADED);
        note.addListener(imapRepository);
        imapRepository.addNote(ident.getUid(), note);

        note = new Note(ident, audit, Note.Classification.PUBLIC, "Hello!!!");
        note.setDescription("This is note one of book one");
        note.setClassification(Note.Classification.CONFIDENTIAL);
        note.addCategories(Tag.createNewTag("TEST"), Tag.createNewTag("WORK"));

        imapRepository.fillUnloadedNote(note);

        assertNull(imapRepository.getEvent(note.getIdentification().getUid()));
        Note filled = imapRepository.getNote(note.getIdentification().getUid());
        assertThat(filled.getDescription(), is(note.getDescription()));
        assertThat(filled.getSummary(), is(note.getSummary()));
        assertThat(filled.getSummary(), is("Hello!!!"));
        assertThat(filled.getAuditInformation(), is(note.getAuditInformation()));
        assertThat(filled.getCategories(), is(note.getCategories()));
        assertThat(filled.getClassification(), is(note.getClassification()));
        assertThat(filled.getColor(), is(note.getColor()));
        assertThat(filled.getIdentification(), is(note.getIdentification()));

        filled.setSummary("HAHAHA");
        assertEquals(EventListener.Type.UPDATE, imapRepository.getEvent(ident.getUid()));
    }

    @Ignore
    @Test
    public void testRemoteChange() {
        try {

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, 2014);
            calendar.set(Calendar.MONTH, Calendar.AUGUST);
            calendar.set(Calendar.DAY_OF_MONTH, 28);

            imapRepository.refresh(calendar.getTime());
            
            Collection<Notebook> notebooks = imapRepository.getNotebooks();
            for(Notebook book : notebooks){
                System.out.println(book.getSummary());
                for(Note note : book.getNotes()){
                    System.out.println(note.getSummary() + " : "+note.getIdentification().getUid());
                }
            }

            //Note attachmentNote = imapRepository.getNote("4440d6b1-5cb9-4e46-91ae-63f9c4ca160a");
            //System.out.println("attachmentNote:" + attachmentNote);
//            Note attachmentNote = imapRepository.getNotebookBySummary("Mit alles").createNote(UUID.randomUUID().toString(), "Attachment note");
//            File file = new File("/tmp/anhang.png");
//            file.createNewFile();
//
//            FileOutputStream fileOut = new FileOutputStream(file);
//            ByteArrayInputStream input = new ByteArrayInputStream(attachmentNote.getAttachments().iterator().next().getData());
//
//            int length = 1024;
//            byte[] bytes = new byte[length];
//            while ((length = input.read(bytes)) != -1) {
//                fileOut.write(bytes, 0, length);
//            }
//            input.close();
//            fileOut.close();

//            byte[] buffer = new byte[1024];
//            InputStream inputStream = getClass().getResourceAsStream("tux-jedi-starwars.png");
//
//            ByteArrayOutputStream output = new ByteArrayOutputStream();
//            int bytes;
//            while ((bytes = inputStream.read(buffer)) != -1) {
//                output.write(buffer, 0, bytes);
//            }
//
//            Attachment att = new Attachment("Testfile", "image/png");
//            att.setData(output.toByteArray());
//
//            inputStream.close();
//            output.close();
//
//            attachmentNote.addAttachments(att);

            //Notebook nb = imapRepository.getNotebookBySummary("Shared Folders/shared/test_notes");
            
            //nb.deleteNote("06e7149b-5db6-4d58-adfd-2d763d4e37ee");
            //imapRepository.deleteNotebook(imapRepository.getNotebookBySummary("Empty").getIdentification().getUid());
            
            //imapRepository.getNote("06e7149b-5db6-4d58-adfd-2d763d4e37ee").removeCategories(imapRepository.getRemoteTags().getTag("Android").getTag());
            //imapRepository.("c5b2050a-d3d5-4cb8-9d98-87fd57cf1ec9");
            
//           Note createNote = nb.createNote(UUID.randomUUID().toString(), "kolabnotes-java note");
//            createNote.addCategories(imapRepository.getRemoteTags().getTag("Android").getTag());
//            createNote.setColor(Colors.BLUE);
//            createNote.setDescription("Hello World!!!!");
//            createNote.setClassification(Note.Classification.CONFIDENTIAL);
//            createNote.addCategories(new Tag("Java"));
            //nb.createNote(UUID.randomUUID().toString(), "Testnote2");
            //nb.createNote(UUID.randomUUID().toString(), "Neuer Versuchnotiz").setDescription("Testbeschreibung");
            //nb.deleteNote("717f5a89-bf9d-44b8-b1d7-2068c5a2a1f6");
            //Notebook nb = imapRepository.getNotebookBySummary("Kolabnotes");
            //        Note createNote = nb.createNote(UUID.randomUUID().toString(), "Testnote");
            //Note createNote = nb.getNote("727c41fc-ec28-11e4-92d0-525477715fa2");
            //        createNote.setClassification(Note.Classification.PRIVATE);
            //        createNote.setDescription("the description");
            //        createNote.addCategories("Linux");
            imapRepository.merge();
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause() != null) {
                e.getCause().printStackTrace();
            }
        }
    }

    @Ignore
    @Test
    public void testRefresh() {
        //This test could fail, it depends on the server settings and of course on the notes on the server!
        imapRepository.refresh();
        imapRepository.getRemoteTags();
        Collection<Notebook> notebooks = imapRepository.getNotebooks();

        for(Notebook nb : notebooks){
            System.out.println(nb.toString());
            if(nb.isShared()){
                System.out.println("Shared from:" + ((SharedNotebook) nb).getUsername());
                System.out.println("Note creation allowed:" + ((SharedNotebook) nb).isNoteCreationAllowed());
                System.out.println("Note modification allowed:" + ((SharedNotebook) nb).isNoteModificationAllowed());
            }
        }
        assertTrue(imapRepository.getTrackedChanges().isEmpty());
        assertFalse(notebooks.isEmpty());
    }

    @Ignore
    @Test
    public void testMerge() {
        Notebook createNotebook = imapRepository.createNotebook("Testingbook-UID", "Testbook");
        Note note = createNotebook.createNote("Testingnote-UID", "Testingnote");
        note.setDescription("Beschreibung");
        note.addCategories(Tag.createNewTag("Work"));
        note.setClassification(Note.Classification.CONFIDENTIAL);

        imapRepository.merge();
    }

    void createTestdata() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.set(Calendar.YEAR, 2014);
        Timestamp now = new Timestamp(cal.getTimeInMillis());
        Identification ident = new Identification("bookOne", "kolabnotes-java");
        AuditInformation audit = new AuditInformation(now, now);
        Notebook book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book One");
        Note note = book.createNote("bookOnenoteOne", "Note one");
        note.setDescription("This is note one of book one");
        note.setClassification(Note.Classification.CONFIDENTIAL);
        note.addCategories(Tag.createNewTag("TEST"), Tag.createNewTag("WORK"));
        note.addListener(imapRepository);
        note.getAuditInformation().setLastModificationDate(cal.getTimeInMillis());
        imapRepository.addNote("bookOnenoteOne", note);
        note = book.createNote("bookOnenoteTwo", "Note two");
        imapRepository.addNote("bookOnenoteTwo", note);
        imapRepository.addNotebook("bookOne", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Identification("bookTwo", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PRIVATE, "Book Two");
        note = book.createNote("bookTwonoteOne", "Note one");
        imapRepository.addNote("bookTwonoteOne", note);
        imapRepository.addNotebook("bookTwo", book);
        book.addListener(imapRepository);
        note.addListener(imapRepository);

        ident = new Identification("bookThree", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Book Three");
        imapRepository.addNotebook("bookThree", book);
        book.addListener(imapRepository);

        ident = new Identification("Notes", "kolabnotes-java");
        audit = new AuditInformation(now, now);
        book = new Notebook(ident, audit, Note.Classification.PUBLIC, "Notes");
        imapRepository.addNotebook("Notes", book);
        book.addListener(imapRepository);
    }
}
