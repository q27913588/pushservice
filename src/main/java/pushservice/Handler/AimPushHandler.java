package pushservice.Handler;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import pushservice.Pojo.AimUserMessage;

public class AimPushHandler extends BaseHandler {

	private String app;
	private String title;
	private String message;
	private boolean isBroadcast;
	private List<AimUserMessage> aimUserMessageList;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy/MM/dd HH:mm:ss", timezone = "GMT+8")
	private Date schedule;
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
	public List<AimUserMessage> getAimUserMessageList() {
		return aimUserMessageList;
	}
	public void setAimUserMessageList(List<AimUserMessage> aimUserMessageList) {
		this.aimUserMessageList = aimUserMessageList;
	}
	public Date getSchedule() {
		return schedule;
	}
	public void setSchedule(Date schedule) {
		this.schedule = schedule;
	}
}
