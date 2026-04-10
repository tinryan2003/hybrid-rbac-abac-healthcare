package org.vgu.billingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.billingservice.model.PaymentRefund;

import java.util.List;

@Repository
public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    List<PaymentRefund> findByPaymentId(Long paymentId);
    List<PaymentRefund> findByInvoiceId(Long invoiceId);
    List<PaymentRefund> findByStatus(String status);
}
