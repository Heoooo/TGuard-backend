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
    public void notifyFraud(Transaction transaction, String ruleName) {
        // 1. Web 알림 저장
        String message = String.format(
                "[이상거래 탐지] Rule: %s\n금액: %.2f원\n위치: %s\n시간: %s",
                ruleName,
                transaction.getAmount(),
                transaction.getLocation(),
                transaction.getTransactionTime()
        );

        notificationRepository.save(Notification.builder()
                .message(message)
                .transaction(transaction)
                .build());

        log.info("웹 알림 저장 완료: {}", message);

        String formattedNumber = formatKoreanNumber(transaction.getUser().getPhoneNumber());
        smsSender.sendSms(formattedNumber, message);
    }

    private String formatKoreanNumber(String phoneNumber) {
        if (phoneNumber.startsWith("0")) {
            return "+82" + phoneNumber.substring(1);
        }
        return "+82" + phoneNumber;
    }

}
