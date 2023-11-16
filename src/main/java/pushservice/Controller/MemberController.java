package pushservice.Controller;

import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pushservice.Application;
import pushservice.Enum.ResultCode;
import pushservice.Handler.AimPushHandler;
import pushservice.Handler.CancelHandler;
import pushservice.Handler.PushHandler;
import pushservice.Handler.RegisterHandler;
import pushservice.Pojo.PushResult;
import pushservice.Pojo.RegisterResult;
import pushservice.Pojo.ResultResponse;
import pushservice.Pojo.TaskResponse;
import pushservice.Service.AppService;
import pushservice.Service.HandlerService;
import pushservice.Service.MemberService;
import pushservice.utils.CommonUtils;

@RestController
public class MemberController {
	
	private Logger logger = Logger.getLogger(MemberController.class);
	
	@Autowired
	MemberService memberService;
	
	@Autowired
	AppService appService;
	
	@Autowired
	HandlerService handlerService;
	
	@PostMapping("/member/register")
	public ResultResponse<RegisterResult> register(@RequestBody RegisterHandler handler) throws InstantiationException, IllegalAccessException {

		if (!handlerService.validate(handler)) {
			logger.error("basic validate fail.");
			return new ResultResponse<RegisterResult>(ResultCode.Unauthorized);
		}
		if (appService.getList().indexOf(handler.getApp()) == -1) {
			return new ResultResponse<RegisterResult>(ResultCode.NotFound);
		}
		
		List<RegisterResult> result = memberService.Register(CommonUtils.clone(handler, RegisterHandler.class));
		
		if (result.size() == 0) {
			new ResultResponse<RegisterResult>(ResultCode.NoContent, CommonUtils.cloneList(result, RegisterResult.class));
		} else if (result.size() != handler.getList().size()) {
			new ResultResponse<RegisterResult>(ResultCode.PartialContent, CommonUtils.cloneList(result, RegisterResult.class));
		}
		
		return new ResultResponse<RegisterResult>(ResultCode.OK, CommonUtils.cloneList(result, RegisterResult.class));
	}
	
	@PostMapping("/member/push")
	public TaskResponse<PushResult> push(@RequestBody PushHandler handler) throws InstantiationException, IllegalAccessException {
		if (!handlerService.validate(handler)) {
			logger.error("basic validate fail.");
			return new TaskResponse<PushResult>(ResultCode.Unauthorized);
		}
		if (appService.getList().indexOf(handler.getApp()) == -1) {
			return new TaskResponse<PushResult>(ResultCode.NotFound);
		}
		
		return memberService.Push(CommonUtils.clone(handler, PushHandler.class));
		
	}
	
	@PostMapping("/member/aim/push")
	public TaskResponse<PushResult> push(@RequestBody AimPushHandler handler) throws InstantiationException, IllegalAccessException {
		if (!handlerService.validate(handler)) {
			logger.error("basic validate fail.");
			return new TaskResponse<PushResult>(ResultCode.Unauthorized);
		}
		if (appService.getList().indexOf(handler.getApp()) == -1) {
			return new TaskResponse<PushResult>(ResultCode.NotFound);
		}
		
		return memberService.AimPush(CommonUtils.clone(handler, AimPushHandler.class));
		
	}
	
	@PostMapping("/member/cancel")
	public ResultResponse<String> cancel(@RequestBody CancelHandler handler) {
		if (!handlerService.validate(handler)) {
			logger.error("basic validate fail.");
			return new ResultResponse<String>(ResultCode.Unauthorized);
		}
		
		String taskId = CommonUtils.clone(handler.getTaskId());
		if (memberService.Cancel(taskId)) {
			return new ResultResponse<String>(ResultCode.OK);
		} else {
			return new ResultResponse<String>(ResultCode.NotFound);
		}
	}
	
	@GetMapping("/member/app")
	public ResultResponse<RegisterResult> app(@RequestParam(value="app", required=true) String handler) throws InstantiationException, IllegalAccessException {
		
		List<RegisterResult> result = memberService.GetByApp(CommonUtils.clone(handler));
		
		return new ResultResponse<RegisterResult>(true, CommonUtils.cloneList(result, RegisterResult.class));
	}
}