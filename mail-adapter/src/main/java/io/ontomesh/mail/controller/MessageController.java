package io.ontomesh.mail.controller;

import io.ontomesh.mail.dto.SimpleMailMessage;
import io.ontomesh.mail.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/messages")
public class MessageController {

    @Autowired
    private MessagingService service;

    @PostMapping("/mails")
    public ResponseEntity<String> send(@RequestBody SimpleMailMessage messageDTO) {
        service.send(messageDTO);
        return ResponseEntity.ok("Mail sent successfully");
    }

}
