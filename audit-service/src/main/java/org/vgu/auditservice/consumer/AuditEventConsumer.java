package org.vgu.auditservice.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.vgu.auditservice.dto.AuditEvent;
import org.vgu.auditservice.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditService auditService;

    /**
     * Consume general audit events
     */
    @RabbitListener(queues = "${rabbitmq.queue.audit.name:audit.events.queue}")
    public void consumeAuditEvent(AuditEvent event) {
        log.info("Received audit event: {}", event.getEventType());
        try {
            auditService.processAuditEvent(event);
            log.debug("Successfully processed audit event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Error processing audit event: {}", event.getEventType(), e);
            // In production, you might want to send to a dead-letter queue
        }
    }

    /**
     * Consume account audit events
     */
    @RabbitListener(queues = "${rabbitmq.queue.audit.account.name:audit.account.queue}")
    public void consumeAccountAuditEvent(AuditEvent event) {
        log.info("Received account audit event: {}", event.getEventType());
        try {
            auditService.processAuditEvent(event);
            log.debug("Successfully processed account audit event");
        } catch (Exception e) {
            log.error("Error processing account audit event", e);
        }
    }

    /**
     * Consume transaction audit events
     */
    @RabbitListener(queues = "${rabbitmq.queue.audit.transaction.name:audit.transaction.queue}")
    public void consumeTransactionAuditEvent(AuditEvent event) {
        log.info("Received transaction audit event: {}", event.getEventType());
        try {
            auditService.processAuditEvent(event);
            log.debug("Successfully processed transaction audit event");
        } catch (Exception e) {
            log.error("Error processing transaction audit event", e);
        }
    }

    /**
     * Consume customer audit events
     */
    @RabbitListener(queues = "${rabbitmq.queue.audit.customer.name:audit.customer.queue}")
    public void consumeCustomerAuditEvent(AuditEvent event) {
        log.info("Received customer audit event: {}", event.getEventType());
        try {
            auditService.processAuditEvent(event);
            log.debug("Successfully processed customer audit event");
        } catch (Exception e) {
            log.error("Error processing customer audit event", e);
        }
    }

    /**
     * Consume employee audit events
     */
    @RabbitListener(queues = "${rabbitmq.queue.audit.employee.name:audit.employee.queue}")
    public void consumeEmployeeAuditEvent(AuditEvent event) {
        log.info("Received employee audit event: {}", event.getEventType());
        try {
            auditService.processAuditEvent(event);
            log.debug("Successfully processed employee audit event");
        } catch (Exception e) {
            log.error("Error processing employee audit event", e);
        }
    }

    /**
     * Consume authorization audit events
     * NOTE: This consumer is DISABLED because AuthorizationEventConsumer handles authorization events
     * with custom conversion logic to handle case-insensitive ResourceType deserialization.
     * Having two consumers on the same queue would cause duplicate processing.
     */
    // @RabbitListener(queues = "${rabbitmq.queue.audit.authorization.name:audit.authorization.queue}")
    // public void consumeAuthorizationAuditEvent(AuditEvent event) {
    //     log.info("Received authorization audit event: {}", event.getEventType());
    //     try {
    //         auditService.processAuditEvent(event);
    //         log.debug("Successfully processed authorization audit event");
    //     } catch (Exception e) {
    //         log.error("Error processing authorization audit event", e);
    //     }
    // }
}
