package pushservice.Handler;

import java.util.List;

import pushservice.Pojo.ReciverBag;
import pushservice.Pojo.TaskPojo;

public interface AppHandler {

	String getAppName();
	
	void sendMessage(TaskPojo task, List<ReciverBag> reciver);
}
