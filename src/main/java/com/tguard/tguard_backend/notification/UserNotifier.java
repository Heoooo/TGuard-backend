package com.tguard.tguard_backend.notification;

import com.tguard.tguard_backend.transaction.entity.Transaction;

public interface UserNotifier {
    void notifyFraud(Transaction transaction, String ruleName);
}
