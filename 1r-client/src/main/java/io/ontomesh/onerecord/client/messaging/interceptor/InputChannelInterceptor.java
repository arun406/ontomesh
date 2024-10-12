package io.ontomesh.onerecord.client.messaging.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.config.GlobalChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@GlobalChannelInterceptor
public class InputChannelInterceptor implements ChannelInterceptor {


    @Override
    public boolean preReceive(MessageChannel channel) {
        return ChannelInterceptor.super.preReceive(channel);
    }

    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {

        MessageHeaders headers = message.getHeaders();
        logHeaders(headers);
        return ChannelInterceptor.super.postReceive(message, channel);
    }

    @Override
    public void afterReceiveCompletion(Message<?> message, MessageChannel channel, Exception ex) {
        logHeaders(message.getHeaders());
        ChannelInterceptor.super.afterReceiveCompletion(message, channel, ex);
    }

    private static void logHeaders(MessageHeaders headers) {
        for (String key : headers.keySet()) {
            String value = getHeader(headers, key);
            log.info("Header Key {}, value : {}", key, value);
        }
    }

    private static String getHeader(MessageHeaders header, Object key) {
        return (String.valueOf(header.get(key)));
    }
}
