package com.ontomesh.onerecord.server.exception;

import lombok.Data;

@Data
public class ServerInfoNotFoundException extends RuntimeException {
    public ServerInfoNotFoundException(String message) {
        super(message);
    }
}
