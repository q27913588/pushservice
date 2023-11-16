package pushservice.Service;

import java.util.List;

import pushservice.Handler.AimPushHandler;
import pushservice.Handler.PushHandler;
import pushservice.Handler.RegisterHandler;
import pushservice.Pojo.PushResult;
import pushservice.Pojo.RegisterResult;
import pushservice.Pojo.TaskResponse;

public interface MemberService {

	List<RegisterResult> Register(RegisterHandler handler);
	
	TaskResponse<PushResult> Push(PushHandler handler) throws InstantiationException, IllegalAccessException;
	
	TaskResponse<PushResult> AimPush(AimPushHandler handler) throws InstantiationException, IllegalAccessException;
	
	List<RegisterResult> GetByApp(String app);
	
	boolean Cancel(String serial);
	
	boolean FirebaseTaskConsumer(String consumerKey);
	
	boolean SmtpTaskConsumer(String consumerKey);
	
	boolean LineTaskConsumer(String consumerKey);
}
