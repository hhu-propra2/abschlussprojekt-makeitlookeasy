package de.propra2.ausleiherino24.email;

import de.propra2.ausleiherino24.model.Case;
import de.propra2.ausleiherino24.model.Conflict;
import de.propra2.ausleiherino24.model.User;
import de.propra2.ausleiherino24.service.UserService;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {

    private EmailConfig config;
    private JavaMailSenderImpl mailSender;
    private SimpleMailMessage message;
    private UserService userService;

    @Autowired
    public EmailSender(final EmailConfig config, final JavaMailSenderImpl mailSender,
            final SimpleMailMessage message, final UserService userService) {
        this.config = config;
        this.mailSender = mailSender;
        this.message = message;
        this.userService = userService;
    }

    public void sendConflictEmail(final Conflict conflict) {
        configureMailSender();

        final User user = userService.findUserByUsername(conflict.getConflictReporterUsername());

        message.setFrom(user.getEmail());
        message.setTo("Clearing@Service.com"); // FakeEmail -> does not matter what goes in here
        message.setSubject("Conflicting Case id: " + conflict.getConflictedCase().getId());
        message.setText(conflict.getConflictDescription());

        mailSender.send(message);
    }

    void sendRemindingEmail(final Case aCase) {
        configureMailSender();

        message.setFrom("Clearing@Service.com");
        message.setTo(aCase.getReceiver().getEmail());
        message.setSubject(
                "Reminder: Article: " + aCase.getArticle().getName()
                        + " has to be returned tomorrow!");
        message.setText("Please do not forget to return the article on time!");

        mailSender.send(message);
    }

    private void configureMailSender() {
        final Properties properties = new Properties();
        properties.putAll(config.getProperties());
        mailSender.setHost(config.getHost());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getUsername());
        mailSender.setPassword(config.getPassword());
        mailSender.setJavaMailProperties(properties);
    }
}
