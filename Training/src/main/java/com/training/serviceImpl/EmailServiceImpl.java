package com.training.serviceImpl;

import com.training.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from:admin@nexorainstitute.in}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otp, String fullName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Nexora Institute - Your OTP for Registration Verification");

            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6; padding: 20px;">
                        <div style="max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; padding: 30px; background-color: #f9f9f9;">
                            <h2 style="color: #0056b3; text-align: center;">Nexora Institute</h2>
                            <p>Dear <strong>%s</strong>,</p>
                            <p>Thank you for initiating the registration process with Nexora Institute.</p>
                            <p>Your One-Time Password (OTP) for account verification is:</p>
                            <div style="text-align: center; margin: 20px 0;">
                                <span style="display: inline-block; padding: 15px 30px; font-size: 24px; font-weight: bold; color: #fff; background-color: #0056b3; border-radius: 5px; letter-spacing: 5px;">%s</span>
                            </div>
                            <p>This OTP is valid for <strong>5 minutes</strong>. Please do not share it with anyone.</p>
                            <p style="font-size: 12px; color: #777; margin-top: 30px; text-align: center;">
                                If you did not request this OTP, please ignore this email.
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(fullName, otp);

            helper.setText(htmlContent, true);

            javaMailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email");
        }
    }
}
