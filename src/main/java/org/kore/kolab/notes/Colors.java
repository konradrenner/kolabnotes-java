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

/**
 *
 * @author Konrad Renner
 */
public enum Colors implements Color {
    WHITE("#FFFFFF"),
    SILVER("#C0C0C0"),
    GRAY("#808080"),
    BLACK("#000000"),
    RED("#FF0000"),
    MAROON("#800000"),
    YELLOW("#FFFF00"),
    OLIVE("#808000"),
    LIME("#00FF00"),
    GREN("#008000"),
    CYAN("#00FFFF"),
    TEAL("#008080"),
    BLUE("#0000FF"),
    NAVY("#000080"),
    MAGENTA("#FF00FF"),
    BROWN("#8B4513"),
    GOLD("#FFD700"),
    ORANGE("FFA500"),
    PURPLE("#800080");

    private final String colorCode;


    private Colors(String colorCode) {
        this.colorCode = colorCode;
    }

    @Override
    public String getHexcode() {
        return this.colorCode;
    }

    /**
     * Searches for an enum value in this class with the given hexCode. If the
     * code is not found a Color.DefaultImpl instance will be returned, with the
     * given hexCode.
     *
     * If the given hexCode is null, null will be returned.
     *
     * @param hexCode
     * @return Color
     */
    public static Color getColor(String hexCode) {
        if (hexCode == null || hexCode.trim().length() == 0) {
            return null;
        }

        for (Color color : Colors.values()) {
            if (color.getHexcode().equalsIgnoreCase(hexCode)) {
                return color;
            }
        }

        return new Color.DefaultImpl(hexCode);
    }
}
