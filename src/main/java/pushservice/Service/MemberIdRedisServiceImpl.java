package pushservice.Service;

import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import pushservice.Pojo.RegisterResult;

@Service
@Qualifier("MemberIdRedisService")
public class MemberIdRedisServiceImpl implements MemberIdService {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	@Autowired
    private RedisTemplate<String, Object> redis;

	@Override
	public void save(String appName, String memberId, String content) {

		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		String contentKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + content;
		String memberIdKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + memberId;
		
		if (StringUtils.isEmpty(content)) {
			remove(appName, memberId);
		} 
		else {
			// 註冊
			logger.info("進行註冊: " + memberId);
			// 移除content原紀錄memberId
			removeOldMemberId(appName, content, memberId);
			// 移除memberId原紀錄contenet
			removeOldContent(appName, memberId, content);
			// 儲存內容
			redis.opsForValue().set(memberIdKey, content);
			redis.opsForValue().set(contentKey, memberId);
		}

	}
	
	@Override
	public boolean remove(String appName, String memberId) {
		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		String memberIdKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + memberId;
		
		// 移除註冊
		logger.info("移除註冊: " + memberId);
		removeOldContent(appName, memberId, "");
		
		return redis.delete(memberIdKey);

	}

	@Override
	public boolean exists(String appName, String memberId) {

		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		return redis.hasKey(PREFIX_MEMBERTOKEN + ":" + appName + ":" + memberId);
	}

	@Override
	public String getContent(String appName, String memberId) {

		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		String tokenKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + memberId;
		return (String)redis.opsForValue().get(tokenKey);
	}
	
	/**
	 * 移除content原儲存memberId
	 * @param appName
	 * @param content
	 * @return
	 */
	private boolean removeOldMemberId(String appName, String content, String newMemberId) {
		
		if (StringUtils.isEmpty(content)) return false;
		
		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		String contentKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + content;
		String oldMemberId = (String)redis.opsForValue().get(contentKey);
		if (!StringUtils.isEmpty(oldMemberId) && !oldMemberId.equalsIgnoreCase(newMemberId)) {
			String oldMKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + oldMemberId;
			String oldContentKey = (String)redis.opsForValue().get(oldMKey);
			// 確認資料相同即刪除
			if (content.equals(oldContentKey)) {
				if (redis.delete(oldMKey)) {
					logger.info("移除原註冊對象: " + oldMemberId + " -> " + content);
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 移除memberId原儲存content
	 * @param appName
	 * @param memberId
	 * @return
	 */
	private boolean removeOldContent(String appName, String memberId, String newContent) {
		
		if (StringUtils.isEmpty(memberId)) return false;
		
		String PREFIX_MEMBERTOKEN = MemberServiceImpl.PREFIX_MEMBERTOKEN;
		String memberIdKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + memberId;
		
		String content = (String)redis.opsForValue().get(memberIdKey);
		if (!StringUtils.isEmpty(content) && !content.equals(newContent)) {
			// 檢查並移除關聯
			String contentKey = PREFIX_MEMBERTOKEN + ":" + appName + ":" + content;
			String refMemberId = (String)redis.opsForValue().get(contentKey);
			if (memberId.equals(refMemberId)) {
				if (redis.delete(contentKey)) {
					logger.info("移除關聯: " + memberId + " -> " + content);
					return true;
				}
			}
		}
		return false;
	}
}
