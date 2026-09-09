package com.gtfs.bustracker.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A small, dependency-free CSV line parser, sufficient for GTFS files:
 * handles double-quoted fields, commas and escaped quotes ("") inside
 * quoted fields. Not a general-purpose CSV/RFC4180 implementation, but
 * enough to safely parse the standard GTFS text files without pulling in
 * an external library just for this.
 */
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

    /**
     * Strips a leading UTF-8 byte-order-mark character if present. GTFS
     * feeds exported from some tools include a BOM on the header line.
     */
    public static String stripBom(String line) {
        if (line != null && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }
}
