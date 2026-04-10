package org.vgu.auditservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.audit.name:audit.exchange}")
    private String auditExchange;

    @Value("${rabbitmq.queue.audit.name:audit.events.queue}")
    private String auditQueue;

    @Value("${rabbitmq.queue.audit.account.name:audit.account.queue}")
    private String accountAuditQueue;

    @Value("${rabbitmq.queue.audit.transaction.name:audit.transaction.queue}")
    private String transactionAuditQueue;

    @Value("${rabbitmq.queue.audit.customer.name:audit.customer.queue}")
    private String customerAuditQueue;

    @Value("${rabbitmq.queue.audit.employee.name:audit.employee.queue}")
    private String employeeAuditQueue;

    @Value("${rabbitmq.queue.audit.authorization.name:audit.authorization.queue}")
    private String authorizationAuditQueue;

    // Transaction Exchange (from transaction service)
    @Value("${rabbitmq.exchange.transaction.name:transaction.exchange}")
    private String transactionExchangeName;

    // Queues for transaction events
    @Value("${rabbitmq.queue.transaction.created:transaction.created.queue}")
    private String transactionCreatedQueue;

    @Value("${rabbitmq.queue.transaction.approved:transaction.approved.queue}")
    private String transactionApprovedQueue;

    @Value("${rabbitmq.queue.transaction.rejected:transaction.rejected.queue}")
    private String transactionRejectedQueue;

    @Value("${rabbitmq.queue.transaction.completed:transaction.completed.queue}")
    private String transactionCompletedQueue;

    // Exchange
    @Bean
    public TopicExchange auditExchange() {
        return new TopicExchange(auditExchange, true, false);
    }

    // Transaction Exchange (to listen from transaction service)
    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(transactionExchangeName, true, false);
    }

    // Queues
    @Bean
    public Queue auditQueue() {
        return new Queue(auditQueue, true);
    }

    @Bean
    public Queue accountAuditQueue() {
        return new Queue(accountAuditQueue, true);
    }

    @Bean
    public Queue transactionAuditQueue() {
        return new Queue(transactionAuditQueue, true);
    }

    @Bean
    public Queue customerAuditQueue() {
        return new Queue(customerAuditQueue, true);
    }

    @Bean
    public Queue employeeAuditQueue() {
        return new Queue(employeeAuditQueue, true);
    }

    @Bean
    public Queue authorizationAuditQueue() {
        return new Queue(authorizationAuditQueue, true);
    }

    // Transaction event queues (to listen from transaction service)
    @Bean
    public Queue transactionCreatedQueue() {
        return new Queue(transactionCreatedQueue, true);
    }

    @Bean
    public Queue transactionApprovedQueue() {
        return new Queue(transactionApprovedQueue, true);
    }

    @Bean
    public Queue transactionRejectedQueue() {
        return new Queue(transactionRejectedQueue, true);
    }

    @Bean
    public Queue transactionCompletedQueue() {
        return new Queue(transactionCompletedQueue, true);
    }

    // Bindings
    @Bean
    public Binding auditBinding() {
        return BindingBuilder.bind(auditQueue())
                .to(auditExchange())
                .with("audit.general");
    }

    @Bean
    public Binding accountAuditBinding() {
        return BindingBuilder.bind(accountAuditQueue())
                .to(auditExchange())
                .with("audit.account");
    }

    @Bean
    public Binding transactionAuditBinding() {
        return BindingBuilder.bind(transactionAuditQueue())
                .to(auditExchange())
                .with("audit.transaction");
    }

    @Bean
    public Binding customerAuditBinding() {
        return BindingBuilder.bind(customerAuditQueue())
                .to(auditExchange())
                .with("audit.customer");
    }

    @Bean
    public Binding employeeAuditBinding() {
        return BindingBuilder.bind(employeeAuditQueue())
                .to(auditExchange())
                .with("audit.employee");
    }

    @Bean
    public Binding authorizationAuditBinding() {
        return BindingBuilder.bind(authorizationAuditQueue())
                .to(auditExchange())
                .with("audit.authorization");
    }

    // Bindings for transaction events (from transaction exchange)
    @Bean
    public Binding transactionCreatedBinding() {
        return BindingBuilder.bind(transactionCreatedQueue())
                .to(transactionExchange())
                .with("transaction.created");
    }

    @Bean
    public Binding transactionApprovedBinding() {
        return BindingBuilder.bind(transactionApprovedQueue())
                .to(transactionExchange())
                .with("transaction.approved");
    }

    @Bean
    public Binding transactionRejectedBinding() {
        return BindingBuilder.bind(transactionRejectedQueue())
                .to(transactionExchange())
                .with("transaction.rejected");
    }

    @Bean
    public Binding transactionCompletedBinding() {
        return BindingBuilder.bind(transactionCompletedQueue())
                .to(transactionExchange())
                .with("transaction.completed");
    }

    // Message converter using Jackson for JSON serialization with Java 8 date/time support
    @Bean
    public MessageConverter jsonMessageConverter() {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        objectMapper.findAndRegisterModules(); // Automatically registers JavaTimeModule
        
        // Register custom deserializer for ResourceType enum (case-insensitive)
        // This fixes the issue where Authorization Service sends "transaction" (lowercase)
        // but the enum expects "TRANSACTION" (uppercase)
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addDeserializer(org.vgu.auditservice.enums.ResourceType.class, 
                              new org.vgu.auditservice.config.ResourceTypeDeserializer());
        objectMapper.registerModule(module);
        
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter(objectMapper);
    }

    // RabbitTemplate
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
