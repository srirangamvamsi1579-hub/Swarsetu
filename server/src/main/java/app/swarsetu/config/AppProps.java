package app.swarsetu.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings under "swarsetu:" in application.yml (all overridable by environment variables). */
@ConfigurationProperties(prefix = "swarsetu")
public record AppProps(
        String jwtSecret,
        int accessMinutes,
        int refreshDays,
        int codeMinutes,
        int maxFailures,
        int lockMinutes,
        String appUrl,
        String mailFrom,
        String smsProvider,
        List<String> corsOrigins,
        String reminderZone,
        Msg91 msg91,
        Twilio twilio) {

    public record Msg91(String authKey, String templateId) {}

    public record Twilio(String accountSid, String authToken, String from) {}
}
