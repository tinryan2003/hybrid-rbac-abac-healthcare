package org.vgu.notificationservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Bean;

@Configuration
public class RabbitMQConfig {
    @Value("${rabbitmq.queue.email.name:email_queue}")
    private String emailQueue;

    @Value("${rabbitmq.exchange.email.name:email.exchange}")
    private String emailExchange;

    @Value("${rabbitmq.binding.email.name:email.routing}")
    private String emailRoutingKey;

    // Transaction queues
    @Value("${rabbitmq.queue.transaction.created:transaction.created.queue}")
    private String transactionCreatedQueue;

    @Value("${rabbitmq.queue.transaction.approved:transaction.approved.queue}")
    private String transactionApprovedQueue;

    @Value("${rabbitmq.queue.transaction.rejected:transaction.rejected.queue}")
    private String transactionRejectedQueue;

    @Value("${rabbitmq.queue.transaction.completed:transaction.completed.queue}")
    private String transactionCompletedQueue;

    @Value("${rabbitmq.exchange.transaction.name:transaction.exchange}")
    private String transactionExchange;

    // Email queue configuration
    @Bean
    public Queue emailQueue() {
        return new Queue(emailQueue, true);
    }

    @Bean
    public TopicExchange emailExchange() {
        return new TopicExchange(emailExchange, true, false);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(emailExchange())
                .with(emailRoutingKey);
    }

    // Transaction queues configuration
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

    @Bean
    public TopicExchange transactionExchange() {
        return new TopicExchange(transactionExchange, true, false);
    }

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

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter converter() {
        // Jackson2JsonMessageConverter for JSON message serialization with Java 8 date/time support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // Automatically registers JavaTimeModule
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
