package com.tguard.tguard_backend.transaction.repository;

import com.tguard.tguard_backend.transaction.entity.Transaction;
import com.tguard.tguard_backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUser(User user);
}