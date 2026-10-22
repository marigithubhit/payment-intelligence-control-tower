package com.mari.iso20022.entity;

import com.mari.iso20022.dto.Iso20022Payment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {
    @Id
    private UUID id;

    @Column(name = "message_id", nullable = false, length = 35)
    private String messageId;
    @Column(name = "creation_date_time", nullable = false)
    private OffsetDateTime creationDateTime;
    @Column(name = "number_of_transactions", nullable = false)
    private int numberOfTransactions;
    @Column(name = "settlement_method", nullable = false, length = 16)
    private String settlementMethod;
    @Column(name = "instruction_id", nullable = false, length = 35)
    private String instructionId;
    @Column(name = "end_to_end_id", nullable = false, length = 35)
    private String endToEndId;
    @Column(name = "transaction_id", nullable = false, unique = true, length = 35)
    private String transactionId;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;
    @Column(name = "debtor_name", nullable = false, length = 140)
    private String debtorName;
    @Column(name = "debtor_iban", nullable = false, length = 34)
    private String debtorIban;
    @Column(name = "debtor_bic", nullable = false, length = 11)
    private String debtorBic;
    @Column(name = "creditor_name", nullable = false, length = 140)
    private String creditorName;
    @Column(name = "creditor_iban", nullable = false, length = 34)
    private String creditorIban;
    @Column(name = "creditor_bic", nullable = false, length = 11)
    private String creditorBic;
    @Column(name = "remittance_information", nullable = false, length = 140)
    private String remittanceInformation;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransaction() {
    }

    public static PaymentTransaction received(Iso20022Payment payment) {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.id = UUID.randomUUID();
        transaction.messageId = payment.messageId();
        transaction.creationDateTime = payment.creationDateTime();
        transaction.numberOfTransactions = payment.numberOfTransactions();
        transaction.settlementMethod = payment.settlementMethod();
        transaction.instructionId = payment.instructionId();
        transaction.endToEndId = payment.endToEndId();
        transaction.transactionId = payment.transactionId();
        transaction.amount = payment.amount();
        transaction.currency = payment.currency();
        transaction.settlementDate = payment.settlementDate();
        transaction.debtorName = payment.debtorName();
        transaction.debtorIban = payment.debtorIban();
        transaction.debtorBic = payment.debtorBic();
        transaction.creditorName = payment.creditorName();
        transaction.creditorIban = payment.creditorIban();
        transaction.creditorBic = payment.creditorBic();
        transaction.remittanceInformation = payment.remittanceInformation();
        transaction.paymentStatus = PaymentStatus.RECEIVED;
        return transaction;
    }

    public static PaymentTransaction create(Iso20022Payment payment, PaymentStatus status) {
        PaymentTransaction transaction = received(payment);
        transaction.paymentStatus = status != null ? status : PaymentStatus.RECEIVED;
        return transaction;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getMessageId() { return messageId; }
    public OffsetDateTime getCreationDateTime() { return creationDateTime; }
    public int getNumberOfTransactions() { return numberOfTransactions; }
    public String getInstructionId() { return instructionId; }
    public String getEndToEndId() { return endToEndId; }
    public String getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public String getDebtorName() { return debtorName; }
    public String getDebtorIban() { return debtorIban; }
    public String getDebtorBic() { return debtorBic; }
    public String getCreditorName() { return creditorName; }
    public String getCreditorIban() { return creditorIban; }
    public String getCreditorBic() { return creditorBic; }
    public String getRemittanceInformation() { return remittanceInformation; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Iso20022Payment toPayment() {
        return new Iso20022Payment(messageId, creationDateTime, numberOfTransactions, settlementMethod,
                instructionId, endToEndId, transactionId, amount, currency, settlementDate,
                debtorName, debtorIban, debtorBic, creditorName, creditorIban, creditorBic,
                remittanceInformation);
    }
}
