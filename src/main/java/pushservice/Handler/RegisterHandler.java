package pushservice.Handler;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import pushservice.Pojo.RegisterPayload;
import pushservice.Pojo.ResultResponse;

@Service
public class RegisterHandler extends BaseHandler {

	private Logger logger = Logger.getLogger(RegisterHandler.class);
	
	private String app;
	
	private List<RegisterPayload> list;
	
    public RegisterHandler() {
    }
    
    public void setApp(String app) {
    	this.app = app;
    }
    
    public String getApp() {
    	return app;
    }
    
    public void setList(List<RegisterPayload> list) {
    	this.list = list;
    }
    
	public List<RegisterPayload> getList() {
		return list;
	}
}
