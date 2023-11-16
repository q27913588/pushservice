package pushservice.Handler;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pushservice.Pojo.AimNotifyMsgRequest;
import pushservice.Pojo.AimNotifyMsgResponse;
import pushservice.Pojo.AimNotifyMsgUser;
import pushservice.Pojo.ReciverBag;
import pushservice.Pojo.TaskPojo;
import pushservice.component.AppUtils;
import pushservice.utils.CommonUtils;

@Component
@Scope("prototype")
public class LineHandler implements AppHandler {

	private Logger logger = Logger.getLogger(LineHandler.class);

	@Autowired
	AppUtils appUtils;
	
	@Autowired
	private ObjectMapper objectMapper;
	
	private String appName;

	@Autowired
	@Qualifier("longHttpClient")
	private OkHttpClient client;
	// AimApi params
	@Value("${aimLine.apiUri}")
	private String aimApiUri;
	private int groupNumber = 1;
	private String systemCode;
	
	@Value("${aimLine.batchSize:100}")
	private int batchSize = 100;
	
	private static final String ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String SECRET = "ESiAIM2019";

    private final Key key;
    private final Cipher cipher;
    
    @Value("${aimLine.key}")
    private String aimKey;
    @Value("${aimLine.iv}")
    private String aimIv;
    
    @Value("${aimLine.pnp-template-id:AIMNS0001,AIMNS0002,AIMNS0003}")
    private List<String> pnpMsgTemplateId;
    
    @PostConstruct
    public void Init() {
    	if (pnpMsgTemplateId == null) pnpMsgTemplateId = new ArrayList<>();
    	if (pnpMsgTemplateId.size() == 0) {
    		pnpMsgTemplateId.add("AIMNS0001");
    		pnpMsgTemplateId.add("AIMNS0002");
    		pnpMsgTemplateId.add("AIMNS0003");
    	}
    	
    	logger.debug("**pnpMsgTemplateId**");
    	for (String id : pnpMsgTemplateId) {
    		logger.debug(id);
    	}
    }
    
    public LineHandler() throws NoSuchAlgorithmException, NoSuchPaddingException {
    	String algorithm = CommonUtils.clone(ALGORITHM);
    	key = new SecretKeySpec(SECRET.getBytes(), algorithm);
        cipher = Cipher.getInstance(algorithm);
    }

	public LineHandler setAppName(String appName) throws IOException {
		this.appName = appName;
		// get default settings

		// get app settings
		Properties prop = appUtils.getAppProperties(this.appName);
		try {
			String groupNumberVal = prop.getProperty("line." + this.appName + ".groupNumber");
			if (!StringUtils.isEmpty(groupNumberVal))
				groupNumber = Integer.parseInt(groupNumberVal);
		} catch (NumberFormatException e) {
			logger.error("GroupNumber NumberformatException.");
		}
		systemCode = prop.getProperty("line." + this.appName + ".systemCode");
		return this;
	}

	@Override
	public String getAppName() {
		return appName;
	}

	/**
	 * Use {@link LineHandler#sendAimMessage(TaskPojo, List)}
	 */
	@Override
	@Deprecated
	public void sendMessage(TaskPojo task, List<ReciverBag> reciver) {
		throw new UnsupportedOperationException();
	}

	public void sendAimMessage(TaskPojo task, List<AimNotifyMsgUser> reciver) {
		// prepare aim request
		AimNotifyMsgRequest payload = new AimNotifyMsgRequest();
		payload.setCheckCode(getCheckCode());
		payload.setGroupNumber(groupNumber);
		payload.setSystemCode(systemCode);
		payload.setMsgTemplateId(task.getTitle());
		int m = getMethodByTemplateId(payload.getMsgTemplateId()); 
		logger.debug("method: " + m);
		payload.setMethod(m);
		// request.setUsers(reciver);
		List<List<AimNotifyMsgUser>> batchUsers = Lists.partition(reciver, batchSize);
			batchUsers.forEach(users -> {
				try {
					payload.setUsers(users);
					String payloadStr = objectMapper.writeValueAsString(payload);
					logger.debug("payload: " + payloadStr);
					Request request = new Request.Builder()
							.url(aimApiUri)
							.header("Accept", MediaType.APPLICATION_JSON_VALUE)
							.post(RequestBody.create(okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE), payloadStr))
							.build();
					
					Response response = client.newCall(request).execute();
					
					if (response.code() != 200) {
						logger.error("Aim Line Request faiure with http status: " + response.code() + "\n" +
								"response: " + response.body().string());
					} else {
						String responseStr = response.body().string();
						logger.debug("AimNotifyMsgResponse: " + responseStr);
						AimNotifyMsgResponse notifyMsgResult = objectMapper.readValue(responseStr, AimNotifyMsgResponse.class);
						if (notifyMsgResult.getReturnCode() == 0) {
							if (notifyMsgResult.getData().getUserStatus() == null)
								logger.error("null 1");
							notifyMsgResult.getData().getUserStatus().forEach(status -> {
								if (status.getResultCode() == 0) {
									task.setSuccessCount(task.getSuccessCount() + 1);
								} else {
									task.setErrorCount(task.getErrorCount() + 1);
								}
							});
						} else {
							logger.error("Aim response error: " + notifyMsgResult.getReturnCode() + " " + notifyMsgResult.getReturnMsg());
						}
						
					}
					
				} catch (IOException e) {
					logger.error(e);
				}
			});
	}

	/**
	 * Aim CheckCode
	 * @return
	 */
	private String getCheckCode() {
		String keyStr = aimKey;
		String ivStr = aimIv;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String value = "ESi" + sdf.format(new Date());
		try {
			String algorithm = CommonUtils.clone(ALGORITHM);
			logger.debug("ALGORITHM:" + algorithm);
			SecretKeySpec skeySpec = new SecretKeySpec(keyStr.getBytes(), algorithm);
			IvParameterSpec iv = new IvParameterSpec(ivStr.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            logger.debug("value: " + value);
            logger.debug("doFinal: " + cipher.doFinal(value.getBytes()));
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new IllegalStateException(e);
        }
	}
	
	private int getMethodByTemplateId(String templateId) {
		
		if (pnpMsgTemplateId != null && !StringUtils.isEmpty(templateId)) {
			for (String id : pnpMsgTemplateId) {
				if (templateId.equalsIgnoreCase(id)) {
					return 2;
				}
			}
		}
		return 1;
	}
}
