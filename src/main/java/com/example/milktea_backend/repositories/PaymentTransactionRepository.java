package com.example.milktea_backend.repositories;

import com.example.milktea_backend.entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    boolean existsByTransactionNo(String transactionNo);
}
