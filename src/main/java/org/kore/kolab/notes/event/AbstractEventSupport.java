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
package org.kore.kolab.notes.event;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Konrad Renner
 */
public class AbstractEventSupport implements EventSupport {

    private final List<EventListener> listener = new ArrayList<EventListener>();

    @Override
    public void addListener(EventListener listener) {
        this.listener.add(listener);
    }

    @Override
    public void firePropertyChange(String uid, EventListener.Type type, String propertyName, Object oldValue, Object newValue) {
        for (EventListener list : listener) {
            list.propertyChanged(uid, type, propertyName, oldValue, newValue);
        }
    }

}
