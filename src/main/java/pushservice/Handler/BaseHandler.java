package pushservice.Handler;

import java.util.Properties;

import org.apache.log4j.Logger;

import pushservice.Application;

public class BaseHandler {

	private Logger logger = Logger.getLogger(BaseHandler.class);
	
	private String app_id;
	private String app_secret;

	public String getApp_id() {
		if (app_id == null)
			return "";
		return app_id;
	}

	public void setApp_id(String app_id) {
		this.app_id = app_id;
	}

	public String getApp_secret() {
		if (app_secret == null)
			return "";
		return app_secret;
	}

	public void setApp_secret(String app_secret) {
		this.app_secret = app_secret;
	}

}
