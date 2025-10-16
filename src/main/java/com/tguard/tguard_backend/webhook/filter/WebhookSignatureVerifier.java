package com.tguard.tguard_backend.webhook.filter;

import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class WebhookSignatureVerifier {

    @Value("${webhook.secret}")
    private String secret;

    public boolean verify(String rawBody, String providedSignature) {
        try {
            if (providedSignature == null || providedSignature.isBlank()) return false;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawBody.getBytes());
            String expected = Hex.encodeHexString(digest);
            return constantTimeEquals(expected, providedSignature);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

}