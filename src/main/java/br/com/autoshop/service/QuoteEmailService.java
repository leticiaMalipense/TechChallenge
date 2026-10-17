package br.com.autoshop.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class QuoteEmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public QuoteEmailService(JavaMailSender mailSender,
                             @Value("${autoshop.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendQuote(String recipient, byte[] quotePdf, String attachmentName, String approvalUrl) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(recipient);
            helper.setSubject("Orçamento da sua ordem de serviço");
            helper.setText("Olá,\n\nSegue em anexo o orçamento da sua ordem de serviço."
                    + "\n\nPara aprovar o orçamento, acesse: " + approvalUrl);
            helper.addAttachment(attachmentName, new ByteArrayResource(quotePdf), "application/pdf");
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Unable to compose quote email", e);
        }

        mailSender.send(message);
    }
}
