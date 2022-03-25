package org.example.config;

import lombok.Data;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Data
public class RabbitMQConfig {

    /**
     * 交换机
     */
    @Value("${mqconfig.order_event_exchange}")
    private String eventExchange;

    /**
     * 延迟队列
     */
    @Value("${mqconfig.order_close_delay_queue}")
    private String orderCloseDelayQueue;

    /**
     * 关单队列
     */
    @Value("${mqconfig.order_close_queue}")
    private String orderCloseQueue;

    /**
     * 进入延迟队列的路由key
     */
    @Value("${mqconfig.order_close_delay_routing_key}")
    private String orderCloseDelayRoutingKey;

    /**
     * 进入死信队列的路由key
     */
    @Value("${mqconfig.order_close_routing_key}")
    private String orderCloseRoutingKey;

    /**
     * 过期时间
     */
    @Value("${mqconfig.ttl}")
    private Integer ttl;

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建交换机 Topic类型，也可以用direct路由
     * 一般一个微服务一个交换机
     */
    @Bean
    public Exchange orderEventExchange() {
        return new TopicExchange(eventExchange, true, false);
    }

    /**
     * 延迟队列
     */
    @Bean
    public Queue orderCloseDelayQueue() {
        Map<String, Object> args = new HashMap<>(3);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", orderCloseRoutingKey);
        args.put("x-message-ttl", ttl);
        return new Queue(orderCloseDelayQueue, true, false, false, args);
    }

    /**
     * 死信队列，普通队列，用于被监听
     */
    @Bean
    public Queue orderCloseQueue() {
        return new Queue(orderCloseQueue, true, false, false);
    }


    /**
     * 第一个队列，即延迟队列的绑定关系建立
     */
    @Bean
    public Binding orderCloseDelayBinding() {
        return new Binding(orderCloseDelayQueue, Binding.DestinationType.QUEUE, eventExchange, orderCloseDelayRoutingKey, null);
    }

    /**
     * 死信队列绑定关系建立
     */
    @Bean
    public Binding orderCloseBinding() {
        return new Binding(orderCloseQueue, Binding.DestinationType.QUEUE, eventExchange, orderCloseRoutingKey, null);
    }


}
