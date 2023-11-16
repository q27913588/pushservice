package pushservice.Pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AimNotifyMsgRequest {

	@JsonProperty("CheckCode")
	private String checkCode;

	@JsonProperty("GroupNumber")
	private int groupNumber;

	@JsonProperty("SystemCode")
	private String systemCode;

	@JsonProperty("MsgTemplateId")
	private String msgTemplateId;

	@JsonProperty("Method")
	private int method;

	@JsonProperty("Users")
	private List<AimNotifyMsgUser> users;

	public String getCheckCode() {
		return checkCode;
	}

	public void setCheckCode(String checkCode) {
		this.checkCode = checkCode;
	}

	public int getGroupNumber() {
		return groupNumber;
	}

	public void setGroupNumber(int groupNumber) {
		this.groupNumber = groupNumber;
	}

	public String getSystemCode() {
		return systemCode;
	}

	public void setSystemCode(String systemCode) {
		this.systemCode = systemCode;
	}

	public String getMsgTemplateId() {
		return msgTemplateId;
	}

	public void setMsgTemplateId(String msgTemplateId) {
		this.msgTemplateId = msgTemplateId;
	}

	public int getMethod() {
		return method;
	}

	public void setMethod(int method) {
		this.method = method;
	}

	public List<AimNotifyMsgUser> getUsers() {
		return users;
	}

	public void setUsers(List<AimNotifyMsgUser> users) {
		this.users = users;
	}
}
