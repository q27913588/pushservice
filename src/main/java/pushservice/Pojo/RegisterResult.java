package pushservice.Pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterResult {

	public RegisterResult() {
		
	}
	
	public RegisterResult(RegisterPayload regster, boolean result) {
		this.memberId = regster.getMemberId();
		this.result = result;
	}
	
	public String memberId;
	public boolean result;
	public String appHandler;
}
