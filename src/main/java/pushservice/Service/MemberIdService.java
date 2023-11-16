package pushservice.Service;

public interface MemberIdService {

	void save(String appName, String memberId, String content);

	boolean exists(String appName, String memberId);

	String getContent(String appName, String memberId);

	boolean remove(String appName, String memberId);

}
