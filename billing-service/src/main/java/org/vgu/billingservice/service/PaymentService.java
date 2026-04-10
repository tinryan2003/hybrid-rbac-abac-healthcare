package org.vgu.billingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vgu.billingservice.dto.PaymentRequest;
import org.vgu.billingservice.dto.PaymentResponse;
import org.vgu.billingservice.exception.BillingNotFoundException;
import org.vgu.billingservice.model.Invoice;
import org.vgu.billingservice.model.Payment;
import org.vgu.billingservice.repository.InvoiceRepository;
import org.vgu.billingservice.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BillingNotFoundException("Payment not found with ID: " + paymentId));
        return mapToPaymentResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request, String keycloakUserId) {
        // Verify invoice exists
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new BillingNotFoundException("Invoice not found with ID: " + request.getInvoiceId()));

        Payment payment = new Payment();
        payment.setInvoiceId(request.getInvoiceId());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(request.getAmount());
        payment.setTransactionId(request.getTransactionId());
        payment.setPaymentReference(request.getPaymentReference());
        payment.setCardLastFour(request.getCardLastFour());
        payment.setCardType(request.getCardType());
        payment.setNotes(request.getNotes());
        payment.setStatus("COMPLETED");
        payment.setReceivedByKeycloakId(keycloakUserId);

        Payment savedPayment = paymentRepository.save(payment);

        // Update invoice payment status
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(request.getAmount());
        invoice.setPaidAmount(newPaidAmount);
        invoice.setOutstandingAmount(invoice.getTotalAmount().subtract(newPaidAmount));

        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus("PAID");
            invoice.setPaidDate(LocalDateTime.now());
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus("PARTIALLY_PAID");
        }

        invoiceRepository.save(invoice);

        return mapToPaymentResponse(savedPayment);
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BillingNotFoundException("Payment not found with ID: " + paymentId));

        payment.setStatus(status);
        Payment updatedPayment = paymentRepository.save(payment);
        return mapToPaymentResponse(updatedPayment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoiceId())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .transactionId(payment.getTransactionId())
                .paymentReference(payment.getPaymentReference())
                .status(payment.getStatus())
                .cardLastFour(payment.getCardLastFour())
                .cardType(payment.getCardType())
                .receivedByBillingClerkId(payment.getReceivedByBillingClerkId())
                .receivedByKeycloakId(payment.getReceivedByKeycloakId())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
