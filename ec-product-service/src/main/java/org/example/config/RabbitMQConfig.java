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
    @Value("${mqconfig.stock_event_exchange}")
    private String eventExchange;

    /**
     * 第一个队列延迟队列，
     */
    @Value("${mqconfig.stock_release_delay_queue}")
    private String stockReleaseDelayQueue;

    /**
     * 第一个队列的路由key
     * 进入队列的路由key
     */
    @Value("${mqconfig.stock_release_delay_routing_key}")
    private String stockReleaseDelayRoutingKey;

    /**
     * 第二个队列，被监听恢复库存的队列
     */
    @Value("${mqconfig.stock_release_queue}")
    private String stockReleaseQueue;

    /**
     * 第二个队列的路由key
     * 即进入死信队列的路由key
     */
    @Value("${mqconfig.stock_release_routing_key}")
    private String stockReleaseRoutingKey;

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
     * 创建交换机 Topic类型，也可以用dirct路由
     * 一般一个微服务一个交换机
     */
    @Bean
    public Exchange stockEventExchange() {
        return new TopicExchange(eventExchange, true, false);
    }

    /**
     * 延迟队列
     */
    @Bean
    public Queue stockReleaseDelayQueue() {
        Map<String, Object> args = new HashMap<>(3);
        args.put("x-message-ttl", ttl);
        args.put("x-dead-letter-exchange", eventExchange);
        args.put("x-dead-letter-routing-key", stockReleaseRoutingKey);
        return new Queue(stockReleaseDelayQueue, true, false, false, args);
    }

    /**
     * 死信队列，普通队列，用于被监听
     */
    @Bean
    public Queue stockReleaseQueue() {
        return new Queue(stockReleaseQueue, true, false, false);
    }

    /**
     * 第一个队列，即延迟队列的绑定关系建立
     */
    @Bean
    public Binding stockReleaseDelayBinding() {
        return new Binding(stockReleaseDelayQueue, Binding.DestinationType.QUEUE, eventExchange, stockReleaseDelayRoutingKey, null);
    }

    /**
     * 死信队列绑定关系建立
     */
    @Bean
    public Binding stockReleaseBinding() {
        return new Binding(stockReleaseQueue, Binding.DestinationType.QUEUE, eventExchange, stockReleaseRoutingKey, null);
    }


}
