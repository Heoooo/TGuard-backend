package com.tguard.tguard_backend.detection.exception;

import com.tguard.tguard_backend.common.exception.CustomException;
import com.tguard.tguard_backend.common.exception.ErrorCode;

public class DetectionResultNotFoundException extends CustomException {
    public DetectionResultNotFoundException() {
        super(ErrorCode.DETECTION_RESULT_NOT_FOUND);
    }
}
