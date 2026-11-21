package com.lifelink.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportServiceTests {

    @Test
    void csvEscapesCommasQuotesAndNewlines() {
        assertEquals("\"a,b\"", ReportService.csv("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", ReportService.csv("say \"hi\""));
        assertEquals("plain", ReportService.csv("plain"));
        assertEquals("", ReportService.csv(null));
    }

    @Test
    void csvNeutralisesSpreadsheetFormulas() {
        assertEquals("'=SUM(A1)", ReportService.csv("=SUM(A1)"));
        assertEquals("'@cmd", ReportService.csv("@cmd"));
        assertEquals("'+1", ReportService.csv("+1"));
    }
}
