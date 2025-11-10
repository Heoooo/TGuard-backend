package com.tguard.tguard_backend.notification;

import com.tguard.tguard_backend.notification.entity.Notification;
import com.tguard.tguard_backend.notification.repository.NotificationRepository;
import com.tguard.tguard_backend.notification.sms.TwilioSmsSender;
import com.tguard.tguard_backend.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultUserNotifier implements UserNotifier {

    private final NotificationRepository notificationRepository;
    private final TwilioSmsSender smsSender;

    @Override
    public void notifyFraud(Transaction transaction, String alertSummary) {
        String message = String.format(
                "[Fraud Alert]%n%s%nAmount: %,.0f KRW%nLocation: %s%nTime: %s",
                alertSummary,
                transaction.getAmount(),
                defaultString(transaction.getLocation(), "unknown"),
                transaction.getTransactionTime()
        );

        notificationRepository.save(Notification.builder()
                .tenantId(transaction.getTenantId())
                .message(message)
                .transaction(transaction)
                .build());

        log.info("Dispatched fraud alert: {}", message);

        String formattedNumber = formatKoreanNumber(transaction.getUser().getPhoneNumber());
        smsSender.sendSms(formattedNumber, message);
    }

    private String formatKoreanNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return phoneNumber;
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.isBlank()) {
            return phoneNumber;
        }

        if (digitsOnly.startsWith("82")) {
            return "+" + digitsOnly;
        }

        if (digitsOnly.startsWith("0")) {
            digitsOnly = digitsOnly.substring(1);
        }

        return "+82" + digitsOnly;
    }

    private String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
