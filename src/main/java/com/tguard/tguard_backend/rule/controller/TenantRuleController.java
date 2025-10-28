package com.tguard.tguard_backend.rule.controller;

import com.tguard.tguard_backend.common.ApiResponse;
import com.tguard.tguard_backend.rule.dto.RuleDescriptionUpdateRequest;
import com.tguard.tguard_backend.rule.dto.RuleResponse;
import com.tguard.tguard_backend.rule.dto.RuleRequest;
import com.tguard.tguard_backend.rule.service.RuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tenant scoped rule management API that can be consumed by operators.
 */
@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
public class TenantRuleController {

    private final RuleService ruleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RuleResponse>>> listRules() {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getAllRules()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> getRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.getRule(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RuleResponse>> createRule(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.addRule(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(@PathVariable Long id,
                                                                @Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.updateRule(id, request)));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<RuleResponse>> toggleRule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.toggleRule(id)));
    }

    @PatchMapping("/{id}/description")
    public ResponseEntity<ApiResponse<RuleResponse>> updateDescription(@PathVariable Long id,
                                                                       @Valid @RequestBody RuleDescriptionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ruleService.updateDescription(id, request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
