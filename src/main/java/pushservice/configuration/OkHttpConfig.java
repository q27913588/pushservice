package pushservice.configuration;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import okhttp3.OkHttpClient;

@Configuration
public class OkHttpConfig {
	
	public static final okhttp3.MediaType JSON = okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE);

	@Value("${app.long-client.timeout:600}")
	int longTimeout;
	
	/**
	 * OkHttp
	 * 
	 * @return
	 */
	@Bean
	@Primary
	public OkHttpClient okHttpClient() {
		return untrustOkHttpClientBuilder().build();
	}
	
	/**
	 * For OCR request
	 * @return
	 */
	@Bean
	@Qualifier("longHttpClient")
	public OkHttpClient longHttpClient() {
		return untrustOkHttpClientBuilder()
			.connectTimeout(longTimeout, TimeUnit.SECONDS)
			.writeTimeout(longTimeout, TimeUnit.SECONDS)
			.readTimeout(longTimeout, TimeUnit.SECONDS)
			.build();
	}

	private OkHttpClient.Builder untrustOkHttpClientBuilder() {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
						throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[] {};
				}
			} };

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
			builder.hostnameVerifier(new HostnameVerifier() {
				@Override
				public boolean verify(String hostname, SSLSession session) {
					return !StringUtils.isEmpty(hostname);
				}
			});

			return builder;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
