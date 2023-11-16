package pushservice.Enum;

public enum TaskResultCode {
	Empty("", "Empty", true),
	Success("000", "Success", true),
	SystemError("001", "System Error");

	private String value;
	
	private String name;
	
	private boolean isSuccess;
	
	TaskResultCode(String value, String name) {
		this.value = value;
		this.name = name;
		this.isSuccess = false;
	}
	
	TaskResultCode(String value, String name, boolean isSuccess) {
		this.value = value;
		this.name = name;
		this.isSuccess = isSuccess;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public static ResultCode findByValue(String value) {
		for (ResultCode c : ResultCode.values() ) {
			if (c.getValue().equalsIgnoreCase(value)) {
				return c;
			}
		}
		return null;
	}
}
