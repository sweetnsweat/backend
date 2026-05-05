package com.capstone.backend.auth.service;

import com.capstone.backend.global.exception.ApiException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class AuthMailService {

    private final JavaMailSender mailSender;
    private final String from;
    private final String fromName;

    public AuthMailService(JavaMailSender mailSender,
                           @Value("${app.mail.from:}") String from,
                           @Value("${app.mail.from-name:sweet & sweat}") String fromName) {
        this.mailSender = mailSender;
        this.from = from;
        this.fromName = fromName;
    }

    public void sendLoginId(String to, String nickname, String loginId) {
        send(
                to,
                "[sweet & sweat] 아이디 찾기 안내",
                """
                        안녕하세요, %s님.

                        요청하신 sweet & sweat 로그인 아이디는 아래와 같습니다.

                        로그인 아이디: %s

                        본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                        """.formatted(nickname, loginId)
        );
    }

    public void sendTemporaryPassword(String to, String nickname, String temporaryPassword) {
        send(
                to,
                "[sweet & sweat] 임시 비밀번호 안내",
                """
                        안녕하세요, %s님.

                        요청하신 sweet & sweat 임시 비밀번호는 아래와 같습니다.

                        임시 비밀번호: %s

                        로그인 후 마이페이지에서 비밀번호를 변경해 주세요.

                        본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                        """.formatted(nickname, temporaryPassword)
        );
    }

    private void send(String to, String subject, String text) {
        if (from == null || from.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL_FROM_NOT_CONFIGURED", "메일 발신자 설정이 없습니다.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MAIL_SEND_FAILED", "메일 발송에 실패했습니다.");
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MAIL_FROM_INVALID", "메일 발신자 이름 설정이 올바르지 않습니다.");
        }
    }
}
