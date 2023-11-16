package pushservice.schedule;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pushservice.Service.MemberService;

@Component
public class LinePushSchedule implements ScheduleRunner {

	private Logger log = Logger.getLogger(this.getClass().getName());

	@Autowired
	private MemberService memberService;
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String serial = UUID.randomUUID().toString();
		//log.info("****** PushSchedule Execute ****** " + serial);
		//TimeUnit.SECONDS.sleep(60);
		/*boolean result =*/
		memberService.LineTaskConsumer(serial);
		//log.info("****** PushSchedule Done ****** " + serial + ":" + result);
	}

	@Override
	public JobKey getJobKey() {
		return JobKey.jobKey("linepushTask");
	}

	@Override
	public JobDataMap initJobData(JobDataMap map) {
		return map;
	}

	@Override
	public Trigger getTrigger() {
		JobKey jk = getJobKey();
		
		return TriggerBuilder.newTrigger()
				.withIdentity(jk.getName(), jk.getGroup())
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(5).repeatForever())
				.build();
	}

}