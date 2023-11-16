package pushservice.component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pushservice.Application;
import pushservice.Handler.LineHandler;

@Component
public class AppUtils {

	private Logger logger = Logger.getLogger(AppUtils.class);
	
	@Value("${app.app-settings:}")
	String appSettingFolder;
	
	@Value("${app.firebase-default:}")
	String filebaseDefault;
	
	public Properties getAppProperties(String name) throws IOException {
		String filename = name + ".properties";

		Properties properties = new Properties();
		InputStream inputStream = getPropertyFile(filename);
		if (inputStream != null) {
			try {
				properties.load(inputStream);
			} finally {
		        inputStream.close();
			}
		} else throw new FileNotFoundException("Cannot found " + filename);

		return properties;
	}
	
	public String getFirebaseProperties() throws IOException {
		
		String json = "";
		InputStream inputStream = getPropertyFile(filebaseDefault);
		if (inputStream != null) {
			try {
				json = convert(inputStream, Charset.defaultCharset());
			} finally {
				inputStream.close();
			}
		}
		return json;
	}
	
	private String convert(InputStream inputStream, Charset charset) throws IOException {
		 if (inputStream != null) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
				String json = br.lines().collect(Collectors.joining(System.lineSeparator()));
				return json;
			}
		 }
		 return null;
	}
	
	private InputStream getPropertyFile(String filename) throws FileNotFoundException {
		
		if (StringUtils.isEmpty(filename)) return null;
		if (filename.contains(File.separator))
			return new FileInputStream(filename);
		if (!StringUtils.isEmpty(appSettingFolder)) {
			Path p = Paths.get(appSettingFolder, filename);
			if (Files.exists(p)) {
				return new FileInputStream(p.toString());
			}
		}
		return Application.class
				.getClassLoader().getResourceAsStream(filename);
	}
}
