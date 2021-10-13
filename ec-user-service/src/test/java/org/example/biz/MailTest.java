package org.example.biz;

import lombok.extern.slf4j.Slf4j;
import org.example.UserApplication;
import org.example.component.MailService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = UserApplication.class)
@Slf4j
public class MailTest {

    @Autowired
    private MailService mailService;

    @Test
    public void testSendMail() {
        mailService.sendMail("1716224950@qq.com", "欢迎你不爱吃鱼的猫丶", "哈哈，这个就是内容");
    }

}
