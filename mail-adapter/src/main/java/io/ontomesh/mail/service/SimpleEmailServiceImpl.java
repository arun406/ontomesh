package io.ontomesh.mail.service;

import io.ontomesh.mail.api.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;

@Slf4j
@Service("SimpleMailService")
public class SimpleEmailServiceImpl implements EmailService {

    private static final String NOREPLY_ADDRESS = "noreply@ontomesh.io";

    @Autowired
    private JavaMailSender emailSender;

    private SimpleMailMessage template;

    @Override
    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(NOREPLY_ADDRESS);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            emailSender.send(message);
        } catch (MailException me) {
            log.error("Exception occurred while sending mail.", me);
        }
    }

    @Override
    public void sendSimpleMessageUsingTemplate(String to, String subject, String... templateModel) {

        String text = String.format(template.getText(), templateModel);
        sendSimpleMessage(to, subject, text);
    }

    @Override
    public void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment) {
        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper messageHelper = null;
        try {

            messageHelper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            messageHelper.setFrom(NOREPLY_ADDRESS);
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(text);

        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
        addAttachment(messageHelper, "attachment1", file);

        emailSender.send(mimeMessage);
    }

    /**
     * adds attachment.
     *
     * @param messageHelper
     * @param fileName
     * @param streamSource
     * @param contentType
     */
    private void addAttachment(MimeMessageHelper messageHelper, String fileName,
                               InputStreamSource streamSource, String contentType) {
        try {
            if (StringUtils.hasText(contentType)) {
                messageHelper.addAttachment(fileName, streamSource, contentType);
            }
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param messageHelper
     * @param fileName
     * @param streamSource
     */
    private void addAttachment(MimeMessageHelper messageHelper, String fileName, InputStreamSource streamSource) {
        try {
            messageHelper.addAttachment(fileName, streamSource);
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
