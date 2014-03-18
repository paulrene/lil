package no.leinstrandil.service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import no.leinstrandil.Config;
import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.listener.AspirinListener;
import org.masukomi.aspirin.listener.ResultState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailService implements AspirinListener {
    private final static Logger log = LoggerFactory.getLogger(MailService.class);

    private Config config;

    public MailService(Config config) {
        this.config = config;

        Aspirin.getConfiguration().setPostmasterEmail("postmaster@leinstrandil.no");
        Aspirin.getConfiguration().setDeliveryDebug(true);
        Aspirin.getConfiguration().setEncoding("UTF-8");

        Aspirin.addListener(this);
    }

    @Override
    public void delivered(String mailId, String recipient, ResultState state, String resultContent) {
        log.info("Mail delivered mailId=" + mailId + ", recipient=" + recipient + ", state=" + state
                + ", resultContent=" + resultContent);
    }

    public void sendNoReplyHtml(String recipientEmail, String subject, String content) {
        content = content.replaceAll("%baseUrl%", config.getBaseUrl());

        MimeMessage message = Aspirin.createNewMimeMessage();
        try {
            message.setFrom(new InternetAddress("no-reply@leinstrandil.no", "Leinstrand Idrettslag", "UTF-8"));
            message.setRecipient(RecipientType.TO, new InternetAddress(recipientEmail));
            message.setContent(content, "text/html");
            message.setSentDate(new Date());
            message.setSubject(subject);
            Aspirin.add(message);

        } catch (AddressException e) {
            log.warn("Could not send reset password email to " + recipientEmail + " due to: " + e.getMessage(), e);
        } catch (MessagingException e) {
            log.warn("Could not send reset password email to " + recipientEmail + " due to: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not send reset password email to " + recipientEmail + " due to: " + e.getMessage(), e);
        }

    }
}
