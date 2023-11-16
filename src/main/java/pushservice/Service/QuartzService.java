package pushservice.Service;

import javax.annotation.PostConstruct;

import org.quartz.SchedulerException;

import pushservice.schedule.ScheduleRunner;

public interface QuartzService {

	@PostConstruct
	public void init();
	
	public boolean create(ScheduleRunner runner) throws SchedulerException;
}
