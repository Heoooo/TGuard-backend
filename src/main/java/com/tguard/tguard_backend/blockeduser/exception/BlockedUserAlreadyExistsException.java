package com.tguard.tguard_backend.blockeduser.exception;

import com.tguard.tguard_backend.common.exception.CustomException;
import com.tguard.tguard_backend.common.exception.ErrorCode;

public class BlockedUserAlreadyExistsException extends CustomException {
    public BlockedUserAlreadyExistsException() {
        super(ErrorCode.INVALID_INPUT);
    }
}
