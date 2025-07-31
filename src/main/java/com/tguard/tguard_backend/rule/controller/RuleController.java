package com.tguard.tguard_backend.rule.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.rule.dto.RuleRequest;
import com.tguard.tguard_backend.rule.dto.RuleResponse;
import com.tguard.tguard_backend.rule.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @PostMapping
    public ResponseEntity<ApiResponse<RuleResponse>> create(@RequestBody @Valid RuleRequest request) {
        RuleResponse result = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getAll() {
        List<RuleResponse> rules = ruleService.getAllRules()
                .stream().map(rule -> new RuleResponse(
                        rule.getId(), rule.getRuleName(), rule.getCondition(),
                        rule.getValue(), rule.getCreatedAt()
                )).toList();

        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> get(@PathVariable Long id) {
        RuleResponse result = ruleService.toResponse(ruleService.getRule(id));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid RuleRequest request
    ) {
        RuleResponse updated = ruleService.updateRule(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
