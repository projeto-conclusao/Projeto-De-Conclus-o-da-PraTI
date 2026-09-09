package com.achadosedevolvidos.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Async("mailTaskExecutor"): envio de e-mail nunca deve travar a thread HTTP que
 * respondeu /forgot-password — a resposta já é uma mensagem genérica que não
 * depende do e-mail ter chegado de fato. Falha de envio só vai para o log (não há
 * requisição HTTP para propagar o erro; ver ItemCreatedEventListener para o mesmo
 * padrão de try/catch em método @Async).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async("mailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Redefinição de senha - Achados e Devolvidos");
        message.setText("Recebemos uma solicitação para redefinir sua senha. Acesse o link abaixo:\n\n"
                + resetLink
                + "\n\nSe você não solicitou isso, ignore este e-mail.");

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Falha ao enviar e-mail de redefinição de senha para {}", toEmail, e);
        }
    }
}
