package com.tguard.tguard_backend.notification.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TwilioSmsSender {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-phone}")
    private String fromPhone;

    public void sendSms(String to, String message) {
        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(fromPhone),
                    message
            ).create();

            log.info("Twilio SMS 전송 성공 → {}: {}", to, message);
        } catch (Exception e) {
            log.error("Twilio SMS 전송 실패", e);
        }
    }
}
