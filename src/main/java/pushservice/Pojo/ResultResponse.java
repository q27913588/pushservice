package pushservice.Pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import pushservice.Enum.ResultCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultResponse<T> {
	public boolean result;
	public String code;
	public String message;
	public List<T> list = null;
	public T data = null;
	
	public ResultResponse(ResultCode rc) {
		code = rc.getValue();
		message = rc.getName();
		result = rc.isSuccess();
	}
	
	public ResultResponse(boolean result) {
		this.result = result;
	}
	
	public ResultResponse(boolean result, T data) {
		this.result = result;
		this.data = data;
	}
	
	public ResultResponse(boolean result, List<T> list) {
		this.result = result;
		this.list = list;
	}
	
	public ResultResponse(ResultCode rc, List<T> list) {
		this(rc);
		this.list = list;
	}
}
