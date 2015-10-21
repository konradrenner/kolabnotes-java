# kolabnotes-java
Library for processing kolab v3 notes (https://wiki.kolab.org/Kolab_3.0_Storage_Format). This library can manage notes locally and also load notes from a remote kolab server via imap. Of course you can extend the repositories for your own needs.

Example usage:
```java

AccountInformation info = AccountInformation.createForHost("imap.kolabserver.com").username("").password("").build();
//First argumet is the desired parser which will be used to parse notes from a storage format into the correct classes
//Second argument is the account information for the IMAP-Server
//Third argument is the folder of the IMAP-Server where the notes are stored (this argument is also the ID of the repository)
RemoteNotesRepository remoteRepository = new ImapNotesRepository(new KolabNotesParserV3(), info, "Notes");

//Example for creating are local repository (notes are just stored in the memory)
Repository repository = new LocalNotesRepository(new KolabNotesParserV3(), "repositoryID");

//Get all notes from the repository
Collection<Note> notes = repository.getNotes();

//Get all notebooks from the repository
Collection<Notebook> notebooks = repository.getNotebooks();

//Get a notebook from the repository
Notebook notebook = repository.getNotebook("NOTEBOOK UID");

//Get a note from the repository
Note note = repository.getNote("NOTE UID");

//Create a note from a notebook
Note note = notebook.createNote("NewNoteUID", "My Summary");

//Sync the changes back to the server
remoteRepository.merge();

```

Get it via jitpack.io
```gradle
repositories {
	    maven {
	        url "https://jitpack.io"
	    }
	}
...

compile 'com.github.konradrenner:kolabnotes-java:[version]'
```
or in Maven:

```xml
<repository>
	    <id>jitpack.io</id>
	    <url>https://jitpack.io</url>
</repository>
...
<dependency> 
	<groupId>com.github.konradrenner</groupId> 
	<artifactId>kolabnotes-java</artifactId> 
	<version>[version]</version> 
</dependency>
``` 
[ ![Download](https://api.bintray.com/packages/konradrenner/maven/kolabnotes-java/images/download.svg) ](https://bintray.com/konradrenner/maven/kolabnotes-java/_latestVersion)

[![Build Status](https://secure.travis-ci.org/konradrenner/kolabnotes-java.png?branch=master)](http://travis-ci.org/konradrenner/kolabnotes-java)

<a href="https://scan.coverity.com/projects/4760">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/4760/badge.svg"/>
</a>
