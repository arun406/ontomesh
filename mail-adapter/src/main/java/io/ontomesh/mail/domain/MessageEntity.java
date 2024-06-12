package io.ontomesh.mail.domain;

import io.ontomesh.mail.enums.MessagePriority;
import io.ontomesh.mail.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@Entity(name = "MESSAGES")
@ToString
public class MessageEntity implements Serializable {

    @Id
    @Column(name = "_ID")
    @GeneratedValue(generator = "message_id_generator", strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(
            name = "message_id_generator",
            sequenceName = "message_id_seq",
            allocationSize = 50
    )
    private Long id;
    @Column(name = "_TO")
    private String to;
    @Column(name = "_TYPE")
    private String type;
    @Column(name = "_CC")
    private String cc;
    @Column(name = "_BCC")
    private String bcc;
    @Column(name = "_FROM")
    private String from;
    @Column(name = "_SUBJECT")
    private String subject;
    @Column(name = "_BODY")
    private String body;
    @Column(name = "_STATUS")
    private MessageStatus status;

    @Column(name = "_PRIORITY")
    private MessagePriority priority;
}
