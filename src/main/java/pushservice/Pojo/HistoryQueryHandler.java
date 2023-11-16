package pushservice.Pojo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import pushservice.Handler.BaseHandler;

public class HistoryQueryHandler extends BaseHandler {

	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy/MM/dd HH:mm:ss", timezone = "GMT+8")
	public Date StartTime;
	
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy/MM/dd HH:mm:ss", timezone = "GMT+8")
	public Date EndTime;
}
