package pushservice.Pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class AimNotifyMsgDataObject {

	private String notifyBatchid;
	
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
	@JsonProperty("userstatus")
	private List<AimNotifyMsgUserParam> userStatus;

	public String getNotifyBatchid() {
		return notifyBatchid;
	}

	public void setNotifyBatchid(String notifyBatchid) {
		this.notifyBatchid = notifyBatchid;
	}

	public List<AimNotifyMsgUserParam> getUserStatus() {
		return userStatus;
	}

	public void setUserStatus(List<AimNotifyMsgUserParam> userStatus) {
		this.userStatus = userStatus;
	}

}
