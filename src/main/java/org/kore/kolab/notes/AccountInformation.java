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
package org.kore.kolab.notes;

import java.io.Serializable;

/**
 *
 * @author Konrad Renner
 */
public final class AccountInformation implements Serializable {

    private String username;
    private String password;
    private final String host;
    private int port = 993;
    private boolean sslEnabled = true;

    private AccountInformation(String host) {
        this.host = host;
    }

    /**
     * Starts the creation of Accountinformations for a Kolab-Server. The
     * default port is 993 and SSL is enabled. The host must be an URL in the
     * form of http[s]://demo.kolabserver.com
     *
     * @param host
     * @return Username
     */
    public static final Username createForHost(String host) {
        return new AccountInformation(host).new DefaultBuilder();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isSSLEnabled() {
        return sslEnabled;
    }

    @Override
    public String toString() {
        return "AccountInformation{" + "username=" + username + ", password=" + password + ", host=" + host + ", port=" + port + ", sslEnabled=" + sslEnabled + '}';
    }

    public interface Username {

        Password username(String user);
    }

    public interface Password {

        Builder password(String password);
    }

    public interface Builder {

        AccountInformation build();

        Builder disableSSL();

        Builder port(int port);
    }

    class DefaultBuilder implements Builder, Password, Username {

        @Override
        public AccountInformation build() {
            return AccountInformation.this;
        }

        @Override
        public Builder disableSSL() {
            AccountInformation.this.sslEnabled = false;
            return this;
        }

        @Override
        public Builder port(int port) {
            AccountInformation.this.port = port;
            return this;
        }

        @Override
        public Builder password(String password) {
            AccountInformation.this.password = password;
            return this;
        }

        @Override
        public Password username(String user) {
            AccountInformation.this.username = user;
            return this;
        }

    }
}
