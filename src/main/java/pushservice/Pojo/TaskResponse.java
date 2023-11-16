package pushservice.Pojo;

import java.util.List;

import pushservice.Enum.ResultCode;

public class TaskResponse<T> extends ResultResponse<T> {

	public String taskId = null;
	
	public TaskResponse(ResultCode rc) {
		super(rc);
	}
	
	public TaskResponse(ResultCode rc, List<T> list) {
		super(rc, list);
	}
	
	public TaskResponse(ResultCode rc, String taskId, List<T> list) {
		super(rc, list);
		this.taskId = taskId;
	}
	
	public TaskResponse(boolean result) {
		super(result);
	}

	public TaskResponse(boolean result, String taskId, List<T> list) {
		super(result, list);
		this.taskId = taskId;
	}
	
	
}
