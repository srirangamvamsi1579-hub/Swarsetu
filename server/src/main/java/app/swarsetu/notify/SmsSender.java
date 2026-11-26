package app.swarsetu.notify;

import app.swarsetu.config.AppProps;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * SMS through MSG91 (India) or Twilio, chosen by SMS_PROVIDER. "console" (the default) only logs: SIMULATED.
 * MSG91 needs a DLT-approved flow template with one variable named VAR1 that receives the message text.
 */
@Component
public class SmsSender {
    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);
    private final AppProps props;
    private final RestClient http = RestClient.create();

    public SmsSender(AppProps props) { this.props = props; }

    public boolean simulated() { return !"msg91".equals(props.smsProvider()) && !"twilio".equals(props.smsProvider()); }

    /** to = E.164, e.g. +919876500001 */
    public void send(String to, String body) {
        switch (String.valueOf(props.smsProvider())) {
            case "msg91" -> {
                Map<String, Object> req = Map.of(
                    "template_id", props.msg91().templateId(),
                    "short_url", "0",
                    "recipients", List.of(Map.of("mobiles", to.replace("+", ""), "VAR1", body)));
                http.post().uri("https://control.msg91.com/api/v5/flow")
                    .header("authkey", props.msg91().authKey())
                    .contentType(MediaType.APPLICATION_JSON).body(req).retrieve().toBodilessEntity();
            }
            case "twilio" -> {
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("To", to);
                form.add("From", props.twilio().from());
                form.add("Body", body);
                http.post().uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", props.twilio().accountSid())
                    .headers(h -> h.setBasicAuth(props.twilio().accountSid(), props.twilio().authToken()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
            }
            default -> log.info("[SIMULATED SMS] to={} : {}", to, body);
        }
    }
}
