package no.leinstrandil.service;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import no.leinstrandil.Config;
import no.leinstrandil.incident.Incident;
import no.leinstrandil.incident.IncidentHub;
import no.leinstrandil.incident.IncidentListener;
import org.masukomi.aspirin.Aspirin;
import org.masukomi.aspirin.listener.AspirinListener;
import org.masukomi.aspirin.listener.ResultState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailService implements AspirinListener, IncidentListener {
    private final static Logger log = LoggerFactory.getLogger(MailService.class);

    private Config config;

    public MailService(Config config) {
        this.config = config;

        Aspirin.getConfiguration().setPostmasterEmail("postmaster@leinstrandil.no");
        Aspirin.getConfiguration().setDeliveryDebug(true);
        Aspirin.getConfiguration().setEncoding("UTF-8");

        Aspirin.addListener(this);

        IncidentHub.addIncidentListener(this);
    }

    @Override
    public void delivered(String mailId, String recipient, ResultState state, String resultContent) {
        log.info("Mail delivered mailId=" + mailId + ", recipient=" + recipient + ", state=" + state
                + ", resultContent=" + resultContent);
    }

    public boolean sendNoReplyHtmlMessage(String recipientEmail, String subject, String content) {
        content = content.replaceAll("%baseUrl%", config.getBaseUrl());

        MimeMessage message = Aspirin.createNewMimeMessage();
        try {
            message.setFrom(new InternetAddress("no-reply@leinstrandil.no", "LeinstrandIL.no", "utf-8"));
            message.setRecipient(RecipientType.TO, new InternetAddress(recipientEmail));
            message.setContent(content, "text/html; charset=utf-8");
            message.setSubject(subject, "utf-8");
            message.setSentDate(new Date());
            Aspirin.add(message);

            return true;
        } catch (AddressException e) {
            log.warn("Could not send email to " + recipientEmail + " due to: " + e.getMessage(), e);
        } catch (MessagingException e) {
            log.warn("Could not send email to " + recipientEmail + " due to: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not send email to " + recipientEmail + " due to: " + e.getMessage(), e);
        }
        return false;
    }

    public boolean sendHtmlMessage(String fromName, String fromAddress, boolean copyMe, Map<String, String> toMap, String subject, String content) {
        content = content.replaceAll("%baseUrl%", config.getBaseUrl());

        MimeMessage message = Aspirin.createNewMimeMessage();
        try {
            message.setFrom(new InternetAddress("no-reply@leinstrandil.no", fromName + " via LeinstrandIL.no", "utf-8"));
            InternetAddress from = new InternetAddress(fromAddress, fromName, "utf-8");
            message.setReplyTo(new Address[] { from });
            if (copyMe) {
                message.addRecipient(RecipientType.CC, from);
            }
            for (String emailAddress : toMap.keySet()) {
                message.addRecipient(RecipientType.TO, new InternetAddress(emailAddress, toMap.get(emailAddress), "utf-8"));
            }
            message.setContent(content, "text/html; charset=utf-8");
            message.setSubject(subject, "utf-8");
            message.setSentDate(new Date());
            Aspirin.add(message);
            return true;
        } catch (AddressException e) {
            log.warn("Could not send email to multiple recipients due to: " + e.getMessage(), e);
        } catch (MessagingException e) {
            log.warn("Could not send email to multiple recipients due to: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not send email to multiple recipients due to: " + e.getMessage(), e);
        }
        return false;

    }

    @Override
    public void incidentOccured(Incident incident) {
        sendNoReplyHtmlMessage("leder@leinstrandil.no", "[Event] " + incident.getClass().getSimpleName(), incident.toReport());
    }
}
