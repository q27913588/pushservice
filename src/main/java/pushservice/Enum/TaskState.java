package pushservice.Enum;

public enum TaskState {
	Undefine(0, "NULL"),
	Schedule(1, "Schedule"),
	Inprocess(2, "Inprocess"),
	Finish(3, "Finish"),
	Cancel(4, "Cancel"),
	Error(5, "Error");
	private int value;
	
	private String name;
	
	TaskState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
