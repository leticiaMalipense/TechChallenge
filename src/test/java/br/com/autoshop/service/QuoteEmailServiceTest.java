package br.com.autoshop.service;

import jakarta.mail.Session;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Multipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteEmailServiceTest {

    private static final byte[] PDF = "%PDF-1.4 test content".getBytes(StandardCharsets.UTF_8);
    private static final String APPROVAL_URL = "https://autoshop.example/approval/token";

    @Mock
    private JavaMailSender mailSender;

    private QuoteEmailService service;

    @BeforeEach
    void setUp() {
        service = new QuoteEmailService(mailSender, "oficina@example.com");
    }

    @Test
    void sendsQuoteWithApprovalLinkAndPdfAttachment() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendQuote("cliente@example.com", PDF, "orcamento.pdf", APPROVAL_URL);

        ArgumentCaptor<MimeMessage> sentMessage = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sentMessage.capture());
        MimeMessage result = sentMessage.getValue();
        result.saveChanges();
        assertEquals("oficina@example.com", result.getFrom()[0].toString());
        assertEquals("cliente@example.com", result.getAllRecipients()[0].toString());
        assertEquals("Orçamento da sua ordem de serviço", result.getSubject());

        Multipart content = (Multipart) result.getContent();
        assertEquals(2, content.getCount());
        assertTrue(textContent(content.getBodyPart(0)).contains(APPROVAL_URL),
                textContent(content.getBodyPart(0)));
        assertEquals("orcamento.pdf", content.getBodyPart(1).getFileName());
        assertEquals("application/pdf", content.getBodyPart(1).getContentType().split(";")[0]);
        assertArrayEquals(PDF, content.getBodyPart(1).getInputStream().readAllBytes());
    }

    @Test
    void propagatesSmtpSendFailure() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(MailSendException.class,
                () -> service.sendQuote("cliente@example.com", PDF, "orcamento.pdf", APPROVAL_URL));
    }

    private static String textContent(Part part) throws Exception {
        Object partContent = part.getContent();
        if (partContent instanceof String text) {
            return text;
        }
        if (partContent instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                text.append(textContent(multipart.getBodyPart(index)));
            }
            return text.toString();
        }
        return "";
    }
}
