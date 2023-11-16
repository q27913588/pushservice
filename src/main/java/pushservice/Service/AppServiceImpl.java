package pushservice.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.io.FilenameUtils;


import pushservice.Application;
import pushservice.Handler.AppHandler;
import pushservice.Handler.FirebaseHandler;
import pushservice.Handler.LineHandler;
import pushservice.Handler.SmtpHandler;
import pushservice.Pojo.AppSwitchResult;
import pushservice.Pojo.HistoryQueryHandler;
import pushservice.Pojo.TaskHistoryCollection;

@Service("AppService")
public class AppServiceImpl implements AppService {

	private Logger logger = Logger.getLogger(AppServiceImpl.class);
	
	@Value("${app.app-settings:}")
	String appSettingFolder;
	
	@Value("${firebase_list:}")
	String firebaseList;
	
	@Value("${smtp.list:}")
	String smtpList;
	
	@Value("${line.list:}")
	String lineList;
	
	private Map<String, AppHandler> appList;
	
	@Autowired
	ApplicationContext applicationContext;
	
	@Autowired
	Environment env;
	
	@Autowired
    private RedisTemplate<String, Object> redis;

	//@PostConstruct
	@Override
	public void init() {
		firebaseList = getProperty("firebase_list", firebaseList);
		smtpList = getProperty("smtp.list", smtpList);
		lineList = getProperty("line.list", lineList);
		logger.info("*********Init**********");
		logger.info("firebase_list: " + firebaseList);
		logger.info("smtp.list: " + smtpList);
		logger.info("line.list: " + lineList);
		List<String> flist = Arrays.asList(firebaseList.split(","));
		
		Map<String, AppHandler> appList = new HashMap<String, AppHandler>();
		// firebase
		flist.forEach(app -> {
			if (firebase_init(app))
				appList.put(app, new FirebaseHandler(app));
    	});
    	// smtp
		List<String> slist = new LinkedList<String>(Arrays.asList(smtpList.split(",")));
		if (slist.size() > 0) {
			slist.removeAll(Arrays.asList("", null));
			slist.forEach(smtp -> {
				try {
					appList.put(smtp, applicationContext.getBean(SmtpHandler.class).setAppName(smtp));
				} catch (BeansException | IOException e) {
					logger.error("App(" + smtp + ") init error.", e);
				}
			});
		}
		// line
		List<String> lList = new LinkedList<String>(Arrays.asList(lineList.split(",")));
		if (lList.size() > 0) {
			lList.removeAll(Arrays.asList("", null));
			lList.forEach(line -> {
				try {
					appList.put(line, applicationContext.getBean(LineHandler.class).setAppName(line));
				} catch (BeansException | IOException e) {
					logger.error("App(" + line + ") init error.", e);
				}
			});
		}
		this.appList = appList;
		logger.info("*********Done**********");
	}
	
	@Override
	public List<String> getList() {
		List<String> keys = new ArrayList<String>();
		appList.keySet().forEach(k -> keys.add(k));
		return keys;
	}

	@Override
	public AppHandler getHandler(String appName) {
		if (appList.containsKey(appName)) {
			return appList.get(appName);
		}
		return null;
	}

	@Override
	public TaskHistoryCollection query(HistoryQueryHandler handler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String appFilter(AppSwitchResult result) {
		logger.info(result.getMemberId(), null);
		
		redis.opsForValue().set("mswitch" + ":" + result.getApp() + ":" + result.getMemberId(), result.getCategoryType());
		
		return "OK";
	}
	
	@Override
	public String getProperty(String key, String defaultValue) {
		
		String val = System.getProperty(key);
		if (!StringUtils.isEmpty(val))
			return val;
		val = env.getProperty(key, defaultValue);
		return val;
	}
	
	public boolean firebase_init(String project) {
    	logger.info("Start init project: " + project);

    	String firebase_adminsdk = getProperty("firebase_" + project, "");
		logger.info("firebase_adminsdk:" + firebase_adminsdk);
		if (StringUtils.isEmpty(firebase_adminsdk)) {
			logger.error("Cannot find property: " + "firebase_" + project);
			return false;
		}
		try {
	    	InputStream refreshToken = null;
	    	if (firebase_adminsdk.contains("/")) {
	    		refreshToken = new FileInputStream(FilenameUtils.normalize(firebase_adminsdk));
	    	} else if (!StringUtils.isEmpty(appSettingFolder)) {
	    		Path fullname = Paths.get(appSettingFolder, FilenameUtils.normalize(firebase_adminsdk));
	    		if (Files.exists(fullname)) {
	    			refreshToken = new FileInputStream(FilenameUtils.normalize(fullname.toString()));
	    		}
	    	}
    		if (refreshToken == null) {
		    	refreshToken = Application.class
						.getClassLoader().getResourceAsStream(firebase_adminsdk);
	    	}
	    	if (refreshToken != null) {
	    		try {
					FirebaseOptions options;
					options = FirebaseOptions.builder()
						    .setCredentials(GoogleCredentials.fromStream(refreshToken))
						    .setDatabaseUrl("https://<DATABASE_NAME>.firebaseio.com/")
						    .build();
					if (project == "default")
						FirebaseApp.initializeApp(options);
					else FirebaseApp.initializeApp(options, project);
					logger.info("Application init done:" + project);
	    		} finally {
	    			refreshToken.close();
	    		}
	    	}
	    	return true;
		} catch (IOException e) {
			logger.error(e);
			logger.error("FirebaseOptions Error.");
		}
		return false;
    }

}
