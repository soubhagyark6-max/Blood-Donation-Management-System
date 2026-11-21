package com.lifelink.controller;

import com.lifelink.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Reports - URL /reports/** is restricted to ADMIN and STAFF. */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public String page(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            LocalDate t = start;
            start = end;
            end = t;
        }
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        model.addAttribute("summary", reports.summary(start, end));
        model.addAttribute("requests", reports.requestsBetween(start, end));
        model.addAttribute("movements", reports.movementsBetween(start, end));
        return "reports/index";
    }

    @GetMapping("/requests.csv")
    public ResponseEntity<byte[]> requestsCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv("lifelink-requests-" + from + "-to-" + to + ".csv", reports.requestsCsv(from, to));
    }

    @GetMapping("/movements.csv")
    public ResponseEntity<byte[]> movementsCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return csv("lifelink-inventory-movements-" + from + "-to-" + to + ".csv", reports.movementsCsv(from, to));
    }

    @GetMapping("/donors.csv")
    public ResponseEntity<byte[]> donorsCsv() {
        return csv("lifelink-donors.csv", reports.donorsCsv());
    }

    private ResponseEntity<byte[]> csv(String filename, String content) {
        byte[] body = ("﻿" + content).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }
}
