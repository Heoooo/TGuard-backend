package com.tguard.tguard_backend.user.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @GetMapping("/me")
    public String me() {
        return "USER OK";
    }
}

