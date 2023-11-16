package pushservice.Service;

import java.util.List;

import pushservice.Handler.AppHandler;
import pushservice.Pojo.AppSwitchResult;
import pushservice.Pojo.HistoryQueryHandler;
import pushservice.Pojo.TaskHistoryCollection;

public interface AppService {
	
	void init();

	List<String> getList();
	
	AppHandler getHandler(String appName);

	TaskHistoryCollection query(HistoryQueryHandler handler);

	String appFilter (AppSwitchResult result);

	String getProperty(String key, String defaultValue);

}
