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

import org.kore.kolab.notes.NotesRepository;

/**
 *
 * @author Konrad Renner
 */
public abstract class ImapConnectionBuilder {

    public interface Hostname {

        public Port hostname(String host);
    }

    public interface Port {

        public User port(String value);
    }

    public interface User {

        public Password user(String value);
    }

    public interface Password {

        public RootFolder password(String value);
    }

    public interface RootFolder {

        public Connect rootFolder(String value);
    }

    public interface Connect {

        public NotesRepository connect();

        public Connect disableSSL();
    }
}
