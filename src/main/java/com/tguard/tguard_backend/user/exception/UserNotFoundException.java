package com.tguard.tguard_backend.user.exception;

import com.tguard.tguard_backend.common.exception.CustomException;
import com.tguard.tguard_backend.common.exception.ErrorCode;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}