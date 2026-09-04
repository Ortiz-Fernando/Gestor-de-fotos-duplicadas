package com.imagedupmanager.web;

import com.imagedupmanager.service.OperationService;
import com.imagedupmanager.web.dto.ApiDtos.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationService operationService;

    public OperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping
    public List<Operation> history() {
        return operationService.history().stream()
                .map(Operation::from)
                .toList();
    }

    @PostMapping("/{id}/undo")
    public ResponseEntity<Map<String, String>> undo(@PathVariable Long id) {
        operationService.undo(id);
        return ResponseEntity.ok(Map.of("message", "Operación deshecha correctamente."));
    }
}
