package io.ontomesh.mail.api;

import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.Map;

public interface ThymeleafEmailService extends EmailService {

    void sendMessageUsingThymeleafTemplate(String to, String subject, Map<String, Object> templateModel)
            throws IOException, MessagingException;

}
