package com.gtfs.bustracker.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvLineParserTest {

    @Test
    void parsesSimpleCommaSeparatedLine() {
        assertArrayEquals(new String[]{"101", "1", "TRIP_1", ""},
                CsvLineParser.parseLine("101,1,TRIP_1,"));
    }

    @Test
    void parsesQuotedFieldsContainingCommas() {
        assertArrayEquals(new String[]{"101", "Main St, Downtown", "3"},
                CsvLineParser.parseLine("101,\"Main St, Downtown\",3"));
    }

    @Test
    void parsesEscapedQuotesInsideQuotedField() {
        assertArrayEquals(new String[]{"He said \"hi\"", "ok"},
                CsvLineParser.parseLine("\"He said \"\"hi\"\"\",ok"));
    }

    @Test
    void handlesEmptyTrailingField() {
        assertArrayEquals(new String[]{"a", "b", ""}, CsvLineParser.parseLine("a,b,"));
    }

    @Test
    void stripsLeadingByteOrderMark() {
        assertEquals("stop_id,stop_name", CsvLineParser.stripBom("\uFEFFstop_id,stop_name"));
    }

    @Test
    void leavesLineWithoutBomUnchanged() {
        assertEquals("stop_id,stop_name", CsvLineParser.stripBom("stop_id,stop_name"));
    }
}
