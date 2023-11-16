package pushservice.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Message.Builder;
import com.google.firebase.messaging.Notification;

import pushservice.Pojo.ReciverBag;
import pushservice.Pojo.TaskPojo;

public class FirebaseHandler implements AppHandler {

	private Logger logger = Logger.getLogger(FirebaseHandler.class); 
	
	private String appName;
	
	public FirebaseHandler(String appName) {
		this.appName = appName;
	}
	
	@Override
	public String getAppName() {
		return appName;
	}
	
	@Override
	public void sendMessage(TaskPojo task, List<ReciverBag> reciver) {
		batchSend(task, reciver);
	}

	private void batchSend(TaskPojo task, List<ReciverBag> token) {

		// See documentation on defining a message payload.
		ApnsConfig apns = ApnsConfig.builder()
			.setAps(Aps.builder()
					.setAlert(ApsAlert.builder()
							.setTitle(task.getTitle())
							.setBody(task.getMessage())
							.build())
					.build())
			.build();
		
		List<Message> messageList = new ArrayList<Message>();
		
		token.forEach(t -> {
			Builder builder = Message.builder()
					.setNotification(Notification.builder()
							.setTitle(task.getTitle())
							.setBody(task.getMessage())
							.build()
						)
					//.setTopic("")
				    .setToken(t.getToken())
				    .setApnsConfig(apns);
			if (task.getApp_type() != null && !task.getApp_type().isNull()) {
				builder.putData("app_type", task.getApp_type().toString());
			}
			messageList.add(builder.build());
		});
		
		logger.info("sent to app(" + task.getApp() + ") notification messages: " + messageList.size());

		// Send a message to the device corresponding to the provided
		// registration token.
		BatchResponse response;
		try {
			response = FirebaseMessaging.getInstance(FirebaseApp.getInstance(task.getApp())).sendAll(messageList);
			task.setSuccessCount(task.getSuccessCount() + response.getSuccessCount());
			task.setErrorCount(task.getErrorCount() + response.getFailureCount());
			// Response is a message ID string.
			logger.info("Success sent message: " + response.getSuccessCount());
			logger.info("Failure sent message: " + response.getFailureCount());
			
			Map<String, String> map = token.stream()
					.collect(HashMap::new, (m, v) -> m.put(v.getToken(), v.getMemberId()), HashMap::putAll);
			
			response.getResponses().forEach(r -> {
				if (!r.isSuccessful() && r.getException() != null) {
					String memberId = "";
					if (map.containsKey(r.getMessageId()))
						memberId = map.get(r.getMessageId());
					logger.error(memberId + ";" + r.getMessageId() + " [ex] " + r.getException().getMessage());
				}
			});
			
		} catch (FirebaseMessagingException e) {
			logger.error("FirebaseMessaging Error.", e);
			task.setErrorCount(task.getErrorCount() + token.size());
		}
	}
}
