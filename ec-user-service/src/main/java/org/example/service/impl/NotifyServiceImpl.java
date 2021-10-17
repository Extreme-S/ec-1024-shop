package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.component.MailService;
import org.example.constant.CacheKey;
import org.example.enums.BizCodeEnum;
import org.example.enums.SendCodeEnum;
import org.example.service.NotifyService;
import org.example.util.CheckUtil;
import org.example.util.CommonUtil;
import org.example.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotifyServiceImpl implements NotifyService {

    @Autowired
    private MailService mailService;

    @Autowired
    private StringRedisTemplate redisTemplate;


    /**
     * 验证码的标题
     */
    private static final String SUBJECT = "ec-1024-shop微服务项目验证码";

    /**
     * 验证码的内容
     */
    private static final String CONTENT = "您的验证码是%s,有效时间是10分钟,打死也不要告诉任何人";

    /**
     * 验证码10分钟有效
     */
    private static final int CODE_EXPIRED = 60 * 1000 * 10;


    /**
     * 前置：判断是否重复发送
     * 1、存储验证码到缓存
     * 2、发送邮箱验证码
     * 后置：存储发送记录
     *
     * @param sendCodeEnum
     * @param to
     * @return
     */
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

    /**
     * 校验验证码
     *
     * @param sendCodeEnum
     * @param to
     * @param code
     * @return
     */
    @Override
    public boolean checkCode(SendCodeEnum sendCodeEnum, String to, String code) {
        String cacheKey = String.format(CacheKey.CHECK_CODE_KEY, sendCodeEnum.name(), to);
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cacheValue)) {
            String cacheCode = cacheValue.split("_")[0];
            if (cacheCode.equals(code)) {
                //删除验证码
                redisTemplate.delete(cacheKey);
                return true;
            }
        }
        return false;
    }
}
