package org.example.component;

public interface MailService {

    /**
     * 发送邮件
     *
     * @param to
     * @param subject
     * @param content
     */
    void sendMail(String to, String subject, String content);
}
