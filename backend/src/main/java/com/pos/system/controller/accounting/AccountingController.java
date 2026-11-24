package com.pos.system.controller.accounting;

import com.pos.system.dto.response.JournalEntryResponse;
import com.pos.system.service.AccountingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting")
public class AccountingController {

    @Autowired
    private AccountingService accountingService;

    @GetMapping("/entries")
    public ResponseEntity<List<JournalEntryResponse>> getAllEntries() {
        List<JournalEntryResponse> entries = accountingService.getAllEntries();
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JournalEntryResponse> getEntryById(@PathVariable Long id) {
        JournalEntryResponse entry = accountingService.getEntryById(id);
        return ResponseEntity.ok(entry);
    }
}
