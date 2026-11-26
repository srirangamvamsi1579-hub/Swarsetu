package app.swarsetu.notify;

import app.swarsetu.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends through SMTP when SMTP_HOST is set; otherwise logs the message and marks it SIMULATED. */
@Component
public class EmailSender {
    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final AppProps props;

    public EmailSender(ObjectProvider<JavaMailSender> mail, AppProps props) { this.mail = mail; this.props = props; }

    public boolean simulated() { return mail.getIfAvailable() == null; }

    public void send(String to, String subject, String body) {
        JavaMailSender m = mail.getIfAvailable();
        if (m == null) {
            log.info("[SIMULATED EMAIL] to={} subject={}\n{}", to, subject, body);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(props.mailFrom());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        m.send(msg);
    }
}
