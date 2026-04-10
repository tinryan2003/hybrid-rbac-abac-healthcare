package org.vgu.notificationservice.util;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.email.name:email.exchange}")
    private String emailExchange;

    @Value("${rabbitmq.binding.email.name:email.routing}")
    private String emailRoutingKey;

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(
                emailExchange,
                emailRoutingKey,
                message);
        log.info("Sent message to RabbitMQ: {}", message);
    }
}