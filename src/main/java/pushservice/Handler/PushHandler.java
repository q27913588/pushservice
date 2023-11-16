package pushservice.Handler;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import pushservice.Pojo.AimUserMessage;

public class PushHandler extends BaseHandler {

	private String app;
	private String title;
	private String message;
	private boolean isBroadcast;
	private List<String> memberIdList;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy/MM/dd HH:mm:ss", timezone = "GMT+8")
	private Date schedule;
	private JsonNode app_type;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getApp() {
		return app;
	}
	public void setApp(String app) {
		this.app = app;
	}
	public boolean isBroadcast() {
		return isBroadcast;
	}
	public void setBroadcast(boolean isBroadcast) {
		this.isBroadcast = isBroadcast;
	}
	public List<String> getMemberIdList() {
		return memberIdList;
	}
	public void setMemberIdList(List<String> memberIdList) {
		this.memberIdList = memberIdList;
	}
	public Date getSchedule() {
		return schedule;
	}
	public void setSchedule(Date schedule) {
		this.schedule = schedule;
	}
	public JsonNode getApp_type() {
		return app_type;
	}
	public void setApp_type(JsonNode app_type) {
		this.app_type = app_type;
	}
}
