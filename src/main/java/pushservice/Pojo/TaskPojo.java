package pushservice.Pojo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

import pushservice.Enum.TaskResultCode;
import pushservice.Enum.TaskState;

public class TaskPojo {

	private String app;
	private boolean isBroadcast;
	private String broadScan;
	private String dataKey;
	private String title;
	private String message;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy/MM/dd HH:mm:ss", timezone = "GMT+8")
	private Date schedule;
	private TaskState status;
	private TaskResultCode resultCode;
	private int total;
	private int successCount;
	private int errorCount;
	private JsonNode app_type;
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
	public String getBroadScan() {
		return broadScan;
	}
	public void setBroadScan(String broadScan) {
		this.broadScan = broadScan;
	}
	public String getDataKey() {
		return dataKey;
	}
	public void setDataKey(String dataKey) {
		this.dataKey = dataKey;
	}
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
	public Date getSchedule() {
		return schedule;
	}
	public void setSchedule(Date schedule) {
		this.schedule = schedule;
	}
	public TaskState getStatus() {
		return status;
	}
	public void setStatus(TaskState status) {
		this.status = status;
	}
	public TaskResultCode getResultCode() {
		return resultCode;
	}
	public void setResultCode(TaskResultCode resultCode) {
		this.resultCode = resultCode;
	}
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	public int getSuccessCount() {
		return successCount;
	}
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}
	public int getErrorCount() {
		return errorCount;
	}
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}
	public JsonNode getApp_type() {
		return app_type;
	}
	public void setApp_type(JsonNode app_type) {
		this.app_type = app_type;
	}
}
