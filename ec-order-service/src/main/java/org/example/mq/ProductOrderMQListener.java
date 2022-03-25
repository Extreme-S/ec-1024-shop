package org.example.mq;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.example.model.OrderMessage;
import org.example.service.ProductOrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RabbitListener(queues = "${mqconfig.order_close_queue}")
public class ProductOrderMQListener {

    @Autowired
    private ProductOrderService productOrderService;

    /**
     * 消费重复消息，幂等性保证
     * 并发情况下如何保证安全
     */
    @RabbitHandler
    public void closeProductOrder(OrderMessage orderMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息：closeProductOrder:{}", orderMessage);
        long msgTag = message.getMessageProperties().getDeliveryTag();
        try {
            boolean flag = productOrderService.closeProductOrder(orderMessage);
            if (flag) {
                channel.basicAck(msgTag, false);
            } else {
                channel.basicReject(msgTag, true);
            }
        } catch (IOException e) {
            log.error("定时关单失败:", orderMessage);
            channel.basicReject(msgTag, true);
        }
    }


}
