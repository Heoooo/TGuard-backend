package com.tguard.tguard_backend.detection.exception;

import com.tguard.tguard_backend.common.exception.CustomException;
import com.tguard.tguard_backend.common.exception.ErrorCode;

public class DetectionFailedException extends CustomException {
    public DetectionFailedException() {
        super(ErrorCode.DETECTION_FAILED);
    }
}
