package com.tguard.tguard_backend.rule.controller;

import com.tguard.tguard_backend.rule.entity.Rule;
import com.tguard.tguard_backend.rule.dto.RuleResponse;
import com.tguard.tguard_backend.rule.service.RuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rules")
@RequiredArgsConstructor
public class AdminRuleController {
    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponse> getRules() { return ruleService.getAllRules(); }

    @GetMapping("/{id}")
    public RuleResponse getRule(@PathVariable Long id) { return ruleService.getRule(id); }

    @PostMapping
    public RuleResponse addRule(@RequestBody Rule rule) { return ruleService.addRule(rule); }

    @PutMapping("/{id}")
    public RuleResponse updateRule(@PathVariable Long id, @RequestBody Rule rule) {
        return ruleService.updateRule(id, rule);
    }

    @DeleteMapping("/{id}")
    public void deleteRule(@PathVariable Long id) { ruleService.deleteRule(id); }
}
