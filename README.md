# kolabnotes-java
Library for processing kolab v3 notes (https://wiki.kolab.org/Kolab_3.0_Storage_Format). This library can manage notes locally and also load notes from a remote kolab server via imap.

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


[![Build Status](https://secure.travis-ci.org/konradrenner/kolabnotes-java.png?branch=master)](http://travis-ci.org/konradrenner/kolabnotes-java)

<a href="https://scan.coverity.com/projects/4760">
  <img alt="Coverity Scan Build Status"
       src="https://scan.coverity.com/projects/4760/badge.svg"/>
</a>
