package pushservice.Handler;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pushservice.Application;
import pushservice.Pojo.ReciverBag;
import pushservice.Pojo.TaskPojo;
import pushservice.component.AppUtils;
import pushservice.utils.CommonUtils;

@Component
@Scope("prototype")
public class SmtpHandler implements AppHandler {

	private Logger logger = Logger.getLogger(SmtpHandler.class);
	
	private String appName = "";
	
	private JavaMailSenderImpl mailSender;
	
	private String from;
	
	@Autowired
	AppUtils appUtils;
	
	public SmtpHandler setAppName(String appName) throws IOException {
		this.appName = appName;
		mailSender = new JavaMailSenderImpl();
		Properties prop = appUtils.getAppProperties(this.appName);
		mailSender.setHost(prop.getProperty("smtp." + appName + ".host"));
		mailSender.setPort(Integer.parseInt(prop.getProperty("smtp." + appName + ".port")));
		mailSender.setUsername(prop.getProperty("smtp." + appName + ".username"));
		String secret = prop.getProperty("smtp." + appName + ".secret", "");
		if (!StringUtils.isEmpty(secret))
			mailSender.setPassword(CommonUtils.clone(secret));
		// https://www.devglan.com/online-tools/jasypt-online-encryption-decryption
		//logger.info(prop.getProperty("smtp." + appName + ".password"));
		
		Properties smtpProps = mailSender.getJavaMailProperties();
		smtpProps.put("mail.transport.protocol", "smtp");
		smtpProps.put("mail.smtp.auth", "true");
		smtpProps.put("mail.smtp.starttls.enable", "false");
		smtpProps.put("mail.debug", "true");
		
		from = prop.getProperty("smtp." + appName + ".from");
		
		return this;
	}
	
	@Override
	public String getAppName() {
		return appName;
	}

	@Override
	public void sendMessage(TaskPojo task, List<ReciverBag> reciver) {
		reciver.forEach(r -> {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(from);
			message.setTo(r.getToken());
			message.setSubject(task.getTitle());
			message.setText(task.getMessage());
			try {
				mailSender.send(message);
				task.setSuccessCount(task.getSuccessCount() + 1);
			} catch (MailException ex) {
				logger.error("mail send error.", ex);
				task.setErrorCount(task.getErrorCount() + 1);
			}
		});

	}

}
