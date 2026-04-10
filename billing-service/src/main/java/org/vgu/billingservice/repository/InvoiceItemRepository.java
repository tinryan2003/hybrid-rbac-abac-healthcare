package org.vgu.billingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vgu.billingservice.model.InvoiceItem;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoice_InvoiceId(Long invoiceId);
    List<InvoiceItem> findByItemType(String itemType);
}
