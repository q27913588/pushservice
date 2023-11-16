package pushservice.schedule;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Trigger;

public interface ScheduleRunner extends Job {

	JobKey getJobKey();
	
	JobDataMap initJobData(JobDataMap map);
	
	Trigger getTrigger();
}
