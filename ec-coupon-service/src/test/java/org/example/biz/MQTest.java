package org.example.biz;

import lombok.extern.slf4j.Slf4j;
import org.example.CouponApplication;
import org.example.model.CouponRecordMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CouponApplication.class)
@Slf4j
public class MQTest {


    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void testSendDelayMsg() {
        rabbitTemplate.convertAndSend("coupon.event.exchange", "coupon.release.delay.routing.key",
                "this is coupon record lock msg");
    }


    @Test
    public void testCouponRecordRelease() {
        CouponRecordMessage message = new CouponRecordMessage();
        message.setOutTradeNo("123456abc");
        message.setTaskId(1L);
        rabbitTemplate.convertAndSend("coupon.event.exchange", "coupon.release.delay.routing.key", message);
    }


}



