package pushservice.Pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AimNotifyMsgUser {

	@JsonProperty("To")
	private String to;
	
	@JsonProperty("Params")
	private Object params;

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public Object getParams() {
		return params;
	}

	public void setParams(Object params) {
		this.params = params;
	}
}
