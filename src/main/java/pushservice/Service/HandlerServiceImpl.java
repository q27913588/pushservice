package pushservice.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pushservice.Handler.BaseHandler;

@Service
public class HandlerServiceImpl implements HandlerService {

	@Value("${app.id:}")
	String id;
	
	@Value("${app.secret:}")
	String secret;
	
	@Override
	public boolean validate(BaseHandler handler) {
		
		return (id.equals(handler.getApp_id()) && secret.equals(handler.getApp_secret()));
	}
}
