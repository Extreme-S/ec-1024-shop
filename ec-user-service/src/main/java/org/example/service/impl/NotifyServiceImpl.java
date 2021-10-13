package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.component.MailService;
import org.example.enums.BizCodeEnum;
import org.example.enums.SendCodeEnum;
import org.example.service.NotifyService;
import org.example.util.CheckUtil;
import org.example.util.CommonUtil;
import org.example.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {


    @Autowired
    private MailService mailService;

    /**
     * 验证码的标题
     */
    private static final String SUBJECT = "小滴课堂验证码";

    /**
     * 验证码的内容
     */
    private static final String CONTENT = "您的验证码是%s,有效时间是60秒,打死也不要告诉任何人";


    @Override
    public JsonData sendCode(SendCodeEnum sendCodeEnum, String to) {
        if (CheckUtil.isEmail(to)) {//邮箱验证码
            String code = CommonUtil.getRandomCode(6);
            mailService.sendMail(to, SUBJECT, String.format(CONTENT, code));
            return JsonData.buildSuccess();
        } else if (CheckUtil.isPhone(to)) {//短信验证码
            //TODO
        }
        return JsonData.buildResult(BizCodeEnum.CODE_TO_ERROR);
    }
}
