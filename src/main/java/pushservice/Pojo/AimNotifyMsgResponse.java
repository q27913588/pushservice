package pushservice.Pojo;

public class AimNotifyMsgResponse {

	private int returnCode;
	
	private String returnMsg;
	
	private AimNotifyMsgDataObject Data;

	public int getReturnCode() {
		return returnCode;
	}

	public void setReturnCode(int returnCode) {
		this.returnCode = returnCode;
	}

	public String getReturnMsg() {
		return returnMsg;
	}

	public void setReturnMsg(String returnMsg) {
		this.returnMsg = returnMsg;
	}

	public AimNotifyMsgDataObject getData() {
		return Data;
	}

	public void setData(AimNotifyMsgDataObject data) {
		Data = data;
	}

}
