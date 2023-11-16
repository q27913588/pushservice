package pushservice.component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pushservice.Application;
import pushservice.Service.AppService;
import pushservice.utils.CommonUtils;

@Component
public class PushApplicationListener implements ApplicationListener<ApplicationReadyEvent> {

	private Logger logger = Logger.getLogger(PushApplicationListener.class);
	
	@Value("${app.setting:}")
	String appSetting;
	
	@Value("${app.app-settings:}")
	String appSettingFolder;
	
	@Autowired
	private AppService appService;
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		logger.debug("onApplicationEvent");

		Properties properties = new Properties();
		String settingFile = CommonUtils.clone(appSetting);
		if (!StringUtils.isEmpty(settingFile)) {
			InputStream inputStream = null;
			
			if (!settingFile.contains(File.separator) && !StringUtils.isEmpty(appSettingFolder)) {
				settingFile = Paths.get(appSettingFolder, settingFile).toString();
			}
			logger.info("settingFile: " + settingFile);
			//if (!settingFile.contains(File.separator)) {
				// resource版本已由AppConfigForJasyptStarter讀取
				//inputStream = Application.class.getClassLoader().getResourceAsStream(settingFile);
			//}
			if (settingFile.contains(File.separator)) {
		        try {
		        	if (inputStream == null)
		        		inputStream = new FileInputStream(settingFile);
		            properties.load(inputStream);
		
		            // 將 properties 的鍵值對載入至環境變數
		            for (String key : properties.stringPropertyNames()) {
		                String value = properties.getProperty(key);
		                System.setProperty(CommonUtils.clone(key), CommonUtils.clone(value));
		                logger.debug(key + ": " + value);
		            }
	            	inputStream.close();
		            logger.info("app.setting讀取完成");
		        } catch (IOException e) {
		            logger.error("app.setting讀取錯誤", e);
		        } finally {
		        	if (inputStream != null) {
		        		try {
		        			inputStream.close();
		        		} catch (IOException e) {
		        			logger.error("inputStream close error", e);;
		        		}
		        	}
		        }
			} else {
				logger.warn("app.setting未指定路徑: " + settingFile);
			}
		} else {
			logger.warn("未設定app.setting");
		}
		
		appService.init();
	}
	

}
