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

/** Bere datoteko GTFS po vrsticah. Položaje stolpcev določi iz glave. */
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

    /** Za vsako vrstico pokliče podano funkcijo. Pogled Row velja le med klicem. */
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

    /** Pogled na polja ene vrstice CSV. */
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
