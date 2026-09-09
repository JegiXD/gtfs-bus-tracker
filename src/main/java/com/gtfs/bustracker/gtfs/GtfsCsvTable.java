package com.gtfs.bustracker.gtfs;

import com.gtfs.bustracker.util.CsvLineParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Streams a single GTFS CSV file row by row without ever holding the whole
 * file in memory, resolving column names to fixed integer indices exactly
 * once (from the header) so each row is only parsed into a plain
 * {@code String[]} - no per-row {@code Map} allocation.
 *
 * This is the key piece of the "efficient memory usage" requirement:
 * callers decide, per file, which rows are worth keeping (e.g. only rows
 * for the requested stop_id) and everything else is discarded as soon as
 * the line is read.
 */
public final class GtfsCsvTable implements AutoCloseable {

    private final BufferedReader reader;
    private final Map<String, Integer> columnIndex;

    private GtfsCsvTable(BufferedReader reader, Map<String, Integer> columnIndex) {
        this.reader = reader;
        this.columnIndex = columnIndex;
    }

    public static GtfsCsvTable open(Path file) throws IOException {
        BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        String headerLine = reader.readLine();
        if (headerLine == null) {
            reader.close();
            throw new IOException("GTFS file is empty: " + file);
        }
        headerLine = CsvLineParser.stripBom(headerLine);
        String[] headers = CsvLineParser.parseLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            index.put(headers[i].trim(), i);
        }
        return new GtfsCsvTable(reader, index);
    }

    public boolean hasColumn(String name) {
        return columnIndex.containsKey(name);
    }

    /**
     * Reads every remaining row and invokes the given consumer with a
     * {@link Row} view. The Row is only valid for the duration of the
     * callback (it is reused/backed by the current line's fields).
     */
    public void forEachRow(Consumer<Row> consumer) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = CsvLineParser.parseLine(line);
            consumer.accept(new Row(fields, columnIndex));
        }
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    /** A lightweight, non-allocating view over one parsed CSV row. */
    public static final class Row {
        private final String[] fields;
        private final Map<String, Integer> columnIndex;

        Row(String[] fields, Map<String, Integer> columnIndex) {
            this.fields = fields;
            this.columnIndex = columnIndex;
        }

        public String get(String columnName) {
            Integer idx = columnIndex.get(columnName);
            if (idx == null || idx >= fields.length) {
                return null;
            }
            String value = fields[idx];
            return value == null || value.isEmpty() ? null : value;
        }
    }
}
