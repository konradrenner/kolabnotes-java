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
 * @author Konrad Renner
 * 
 */
public interface Color extends Serializable {

    /**
     * Gets the 6 digit hexadecimal rgb web color code.
     *
     * @return String
     */
    String getHexcode();

    static class DefaultImpl implements Color {

        private final String hexCode;

        public DefaultImpl(String hexCode) {
            this.hexCode = hexCode;
        }

        @Override
        public String getHexcode() {
            return hexCode;
        }

        @Override
        public String toString() {
            return "DefaultImpl{" + "hexCode=" + hexCode + '}';
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 97 * hash + (this.hexCode != null ? this.hexCode.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final DefaultImpl other = (DefaultImpl) obj;
            if ((this.hexCode == null) ? (other.hexCode != null) : !this.hexCode.equals(other.hexCode)) {
                return false;
            }
            return true;
        }

    }
}
