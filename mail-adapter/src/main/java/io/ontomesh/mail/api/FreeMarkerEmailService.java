package io.ontomesh.mail.api;

import freemarker.template.TemplateException;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.util.Map;

public interface FreeMarkerEmailService extends EmailService {

    void sendMessageUsingFreeMarkerTemplate(String to, String subject, Map<String, Object> templateModel)
            throws IOException, TemplateException, MessagingException;
}
