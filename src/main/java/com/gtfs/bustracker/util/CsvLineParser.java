package com.gtfs.bustracker.util;

import java.util.ArrayList;
import java.util.List;

/** Razčleni vrstico CSV ter upošteva vejice in podvojene narekovaje v narekovanih poljih. */
public final class CsvLineParser {

    private CsvLineParser() {
    }

    public static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    boolean nextIsQuote = (i + 1 < line.length()) && line.charAt(i + 1) == '"';
                    if (nextIsQuote) {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    /** Odstrani začetno oznako BOM, če je prisotna. */
    public static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}
