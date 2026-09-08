package com.jurisfacil.shared.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailGateway implements EmailGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEmailGateway.class);

    @Override
    public void send(String recipient, String subject, String body) {
        LOGGER.info("email_gateway recipient={} subject={} body={}", recipient, subject, body);
    }
}
