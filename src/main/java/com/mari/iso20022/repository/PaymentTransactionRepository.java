package com.mari.iso20022.repository;

import com.mari.iso20022.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import com.mari.iso20022.entity.PaymentStatus;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByTransactionId(String transactionId);
    boolean existsByTransactionId(String transactionId);
    boolean existsByTransactionIdAndIdNot(String transactionId, UUID id);
    List<PaymentTransaction> findByDebtorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
            String debtorIban, OffsetDateTime from, OffsetDateTime to, UUID id);
    List<PaymentTransaction> findByDebtorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualOrderByCreationDateTimeDesc(
            String debtorIban, OffsetDateTime from, OffsetDateTime to);
    List<PaymentTransaction> findByDebtorIbanIgnoreCaseAndCreditorIbanIgnoreCaseAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
            String debtorIban, String creditorIban, OffsetDateTime from, OffsetDateTime to, UUID id);
    List<PaymentTransaction> findByDebtorIbanIgnoreCaseAndPaymentStatusAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
            String debtorIban, PaymentStatus status, OffsetDateTime from, OffsetDateTime to, UUID id);
    List<PaymentTransaction> findByDebtorIbanIgnoreCaseAndCreditorIbanIgnoreCaseAndPaymentStatusAndCreationDateTimeGreaterThanEqualAndCreationDateTimeLessThanEqualAndIdNotOrderByCreationDateTimeDesc(
            String debtorIban, String creditorIban, PaymentStatus status, OffsetDateTime from, OffsetDateTime to, UUID id);
}
