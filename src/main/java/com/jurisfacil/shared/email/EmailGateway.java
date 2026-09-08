package com.jurisfacil.shared.email;

/** Transversal gateway for outbound e-mail integrations. */
public interface EmailGateway {

    void send(String recipient, String subject, String body);
}
