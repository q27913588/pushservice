package pushservice.Controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import pushservice.Enum.ResultCode;
import pushservice.Pojo.AppSwitchResult;
import pushservice.Pojo.HistoryQueryHandler;
import pushservice.Pojo.ResultResponse;
import pushservice.Pojo.TaskHistoryCollection;
import pushservice.Service.AppService;
import pushservice.Service.HandlerService;
import pushservice.utils.CommonUtils;

@RestController
public class AppController {

	private Logger logger = Logger.getLogger(AppController.class);
	
	@Autowired
	AppService appService;
	
	@Autowired
	HandlerService handlerService;
	
	public ResultResponse<TaskHistoryCollection> history(@RequestBody HistoryQueryHandler handler) throws InstantiationException, IllegalAccessException {
		if (!handlerService.validate(handler)) {
			logger.error("basic validate fail.");
			return new ResultResponse<TaskHistoryCollection>(ResultCode.Unauthorized);
		}
		
		TaskHistoryCollection result = appService.query(CommonUtils.clone(handler, HistoryQueryHandler.class));
		return new ResultResponse<TaskHistoryCollection>(true, CommonUtils.clone(result, TaskHistoryCollection.class));
	}

	@PostMapping("/member/switch")
	public ResultResponse<String> push(@RequestBody AppSwitchResult result) throws InstantiationException, IllegalAccessException {
		if (result.getApp() == null || result.getCategoryType() == null) {
			return new ResultResponse<String>(false, "app or categoryType is null.");
		}
		String status = appService.appFilter(CommonUtils.clone(result, AppSwitchResult.class));
		
		return new ResultResponse<String>(true, CommonUtils.clone(status));
		
	}
}
