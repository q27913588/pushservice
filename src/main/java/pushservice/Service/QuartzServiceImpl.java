package pushservice.Service;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pushservice.schedule.FirebasePushSchedule;
import pushservice.schedule.LinePushSchedule;
import pushservice.schedule.ScheduleRunner;
import pushservice.schedule.SmtpPushSchedule;

@Service("quartzService")
@Transactional
public class QuartzServiceImpl implements QuartzService {

	private Logger log = Logger.getLogger(this.getClass().getName());
	
	@Autowired
	private Scheduler scheduler;
	
	@Autowired
	private FirebasePushSchedule firebasePushSchedule;
	
	@Autowired
	private SmtpPushSchedule smtpPushSchedule;
	
	@Autowired
	private LinePushSchedule linePushSchedule;
	
	@PostConstruct
	@Override
	public void init() {
		log.info("******init******");
		try {
			this.create(firebasePushSchedule);
			this.create(smtpPushSchedule);
			this.create(linePushSchedule);
			log.info("******done******\"");
		} catch (SchedulerException e) {
			log.error("scheduler init error", e);
		}
	}

	@Override
	public boolean create(ScheduleRunner runner) throws SchedulerException {
		JobKey jk = runner.getJobKey();
		if (scheduler.checkExists(jk)) {
			log.error("task schedule exists. name=" + jk.getName() + ", group=" + jk.getGroup());
			return false;
		}
		log.trace("runner: " + runner.getClass().getName());
		JobDetail job = JobBuilder.newJob(runner.getClass())
				.withIdentity(jk)
				.build();
		runner.initJobData(job.getJobDataMap());
		
		Trigger trigger = runner.getTrigger();
		if (trigger == null) {
			return false;
		}
		
		scheduler.scheduleJob(job, trigger);
		
		return true;
	}

}
