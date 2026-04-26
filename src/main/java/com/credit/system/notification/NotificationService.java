package com.credit.system.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 通知服务 —— 提供邮件、站内信等通知能力。
 * <p>
 * 当前实现邮件发送，可扩展为短信、企业微信、钉钉等渠道。
 * </p>
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 发送邮件通知
     *
     * @param to      收件人
     * @param subject 主题
     * @param body    正文
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("邮件发送成功 - 收件人: {}, 主题: {}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败 - 收件人: {}, 主题: {}", to, subject, e);
        }
    }

    /**
     * 发送合同签署通知
     */
    public void notifyContractSigned(String customerEmail, String contractNo) {
        sendEmail(customerEmail,
                "合同签署成功通知",
                String.format("尊敬的客户，您的合同（编号: %s）已成功签署。", contractNo));
    }

    /**
     * 发送还款提醒
     */
    public void notifyRepaymentDue(String customerEmail, String contractNo,
                                   String dueDate, String amount) {
        sendEmail(customerEmail,
                "还款提醒",
                String.format("尊敬的客户，您的合同（编号: %s）将于 %s 到期，应还金额: %s 元。",
                        contractNo, dueDate, amount));
    }

    /**
     * 发送逾期警告
     */
    public void notifyOverdue(String customerEmail, String contractNo,
                              int overdueDays, String overdueAmount) {
        sendEmail(customerEmail,
                "逾期警告",
                String.format("尊敬的客户，您的合同（编号: %s）已逾期 %d 天，逾期金额: %s 元，请尽快还款。",
                        contractNo, overdueDays, overdueAmount));
    }
}
