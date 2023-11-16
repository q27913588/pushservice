package pushservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.util.StringUtils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import pushservice.Service.QuartzService;
import pushservice.schedule.QuartzSchedulerListener;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {

	private static Logger logger = Logger.getLogger(Application.class);
	
	@Autowired
	QuartzService quartzService;

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		BasicConfigurator.configure();
		Security.addProvider(new BouncyCastleProvider());
		return application.sources(Application.class);
	}

    public static void main(String[] args) {
    	BasicConfigurator.configure();
    	Security.addProvider(new BouncyCastleProvider());
    	SpringApplication.run(Application.class, args);
    }
    
	public static String convert(InputStream inputStream, Charset charset) throws IOException {
		 
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}
	
    /*
     * ??
     */
    @Bean
    public SchedulerFactoryBean schedulerFactory(ApplicationContext applicationContext) {
    	SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
    	AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
    	jobFactory.setApplicationContext(applicationContext);
    	factoryBean.setSchedulerListeners(applicationContext.getBean(QuartzSchedulerListener.class));
    	factoryBean.setJobFactory(jobFactory);;
    	factoryBean.setApplicationContextSchedulerContextKey("applicationContext");;
    	factoryBean.setConfigLocation(new ClassPathResource("quartz.properties"));
    	return factoryBean;
    }

}
