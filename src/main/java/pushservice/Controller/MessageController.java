package pushservice.Controller;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;

import pushservice.Application;
import pushservice.Handler.MessageHandler;
import pushservice.Pojo.MsgResult;
import pushservice.Pojo.ResultResponse;
import pushservice.Service.HandlerService;
import pushservice.component.AppUtils;

@RestController
public class MessageController {

	private Logger logger = Logger.getLogger(MessageController.class);
	
	@Autowired
	AppUtils appUtils;
	
	@Autowired
	HandlerService handlerService;
	
	@PostMapping("/message")
    public ResultResponse<MsgResult> message(@RequestBody MessageHandler handler) {
		try {
			
			handlerService.validate(handler);
			return handler.push();
		} catch (FileNotFoundException e) {
			return new ResultResponse<MsgResult>(false);
		}
    }
	
	@GetMapping("/test")
	public String message() throws IOException {
		String jsonStr = appUtils.getFirebaseProperties();
		if (StringUtils.isEmpty(jsonStr)) return "firebase configure load failure.";
		JsonNode node;
		try {
			node = new ObjectMapper().readTree(jsonStr);
		} catch (IOException e) {
			return "firebase configure load failure.";
		}
		/*
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-cbn7f%40sklbeacon-5f585.iam.gserviceaccount.com"

		 */
		String[] checkAttrs = new String[] {"auth_uri", "token_uri", "auth_provider_x509_cert_url", "client_x509_cert_url"};
		List<String> list = new ArrayList<String>();
		list.add("fcm.googleapis.com");
		for (String attr : checkAttrs) {
			if (node.has(attr)) {
				String val = node.get(attr).asText();
				URL url;
				try {
					url = new URL(val);
					val = url.getHost();
					if (!list.contains(val))
						list.add(val);
				} catch (MalformedURLException e) {
					logger.error("MalformedURLException: " + e.getMessage(), e);
				}
			}
		}
		
		List<String> result = new ArrayList<String>();
		for (String domain : list) {
			if (SocketPortCheck(domain, 443))
				result.add("\"" + domain + "\":\"checked\"");
			else result.add("\"" + domain + "\":\"failure\"");
		}
		return result.toString();
	}
	
	private boolean SocketPortCheck(String domain, int port) {
		Socket s = null;
		try {
			s = new Socket(domain, port);
			if (s != null) {
	            try {
	                s.close();
	            } catch (IOException e) {
	                throw new RuntimeException("Socket close failure." , e);
	            }
			}
			return true;
		} catch (IOException e) {
			logger.error(e.toString());
			return false;
		}
	}
}
