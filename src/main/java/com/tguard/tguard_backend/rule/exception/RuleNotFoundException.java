package com.tguard.tguard_backend.rule.exception;

import com.tguard.tguard_backend.common.exception.CustomException;
import com.tguard.tguard_backend.common.exception.ErrorCode;

public class RuleNotFoundException extends CustomException {
    public RuleNotFoundException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
