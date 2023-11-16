package pushservice.Entity;

import java.io.Serializable;

public class MemberIdKey implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1679777859344226799L;

	public MemberIdKey() {}
	public MemberIdKey(String appName, String memberId) {
		this.appName = appName;
		this.memberId = memberId;
	}
	
	private String appName;
	
	private String memberId;

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}
	
	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        MemberIdKey other = (MemberIdKey) o;
        
        // 比較複合 ID 的每個部分是否相等
        if (appName != null ? !appName.equals(other.appName) : other.appName != null) {
            return false;
        }
        if (memberId != null ? !memberId.equals(other.memberId) : other.memberId != null) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = appName != null ? appName.hashCode() : 0;
        result = 31 * result + (memberId != null ? memberId.hashCode() : 0);
        return result;
    }
}
