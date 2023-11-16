package pushservice.Handler;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import pushservice.Application;
import pushservice.Pojo.MsgPayload;
import pushservice.Pojo.MsgResult;
import pushservice.Pojo.ResultResponse;

public class MessageHandler extends BaseHandler {
	
	private Logger logger = Logger.getLogger(MessageHandler.class);
	
	private List<MsgPayload> list;

    public MessageHandler() {
    }

    public void setList(List<MsgPayload> list) {
    	this.list = list;
    }
    
	public List<MsgPayload> getList() {
		return list;
	}
	
	public ResultResponse<MsgResult> push() throws FileNotFoundException {
		if (list == null || list.size() == 0)
			return new ResultResponse(false);

		ArrayList<MsgResult> result = new ArrayList<MsgResult>();

		list.forEach((m) -> {
			MsgResult r = new MsgResult();
			r.token = m.token;
			r.msgId = "";
			if ((m.title == null || m.title.isEmpty())
					&& (m.message == null || m.message.isEmpty())) {
				r.result = false;
			}
			else {
				// See documentation on defining a message payload.
				ApnsConfig apns = ApnsConfig.builder()
					.setAps(Aps.builder()
							.setAlert(ApsAlert.builder()
									.setTitle(m.title)
									.setSubtitle(m.message)
									.build())
							.build())
					.build();
				Message message = Message.builder()
					//.setNotification(new Notification( m.title, m.text))
				    .putData("title", m.title)
				    .putData("message", m.message)
				    .setToken(m.token)
				    .setApnsConfig(apns)
				    .build();
				
				logger.info("send:" + m.title + ":" + m.message + ":" + m.token);
	
				// Send a message to the device corresponding to the provided
				// registration token.
				String response;
				try {
					response = FirebaseMessaging.getInstance().send(message);
					// Response is a message ID string.
					logger.info("Successfully sent message: " + response);
					r.msgId = response;
					r.result = true;
				} catch (FirebaseMessagingException e) {
					// TODO Auto-generated catch block
					logger.error(e);
					logger.error("FirebaseMessaging Error.");
					r.result = false;
				}
			}
			result.add(r);
		});
		
		logger.info(list.size());
		return new ResultResponse(true, result);
	}

}
