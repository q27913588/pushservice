package pushservice.schedule;

import org.apache.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.ee.jmx.jboss.QuartzService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QuartzSchedulerListener implements SchedulerListener {

	private Logger log = Logger.getLogger(this.getClass().getName());
	
	@Override
	public void jobScheduled(Trigger trigger) {
		log.trace("jobScheduled trigger:" + trigger.toString());

	}

	@Override
	public void jobUnscheduled(TriggerKey triggerKey) {
		log.trace("jobUnscheduled triggerKey:" + triggerKey);

	}

	@Override
	public void triggerFinalized(Trigger trigger) {
		log.trace("triggerFinalized trigger:" + trigger);

	}

	@Override
	public void triggerPaused(TriggerKey triggerKey) {
		log.trace("triggerPaused triggerKey:" + triggerKey);

	}

	@Override
	public void triggersPaused(String triggerGroup) {
		log.trace("triggersPaused triggerGroup:" + triggerGroup);

	}

	@Override
	public void triggerResumed(TriggerKey triggerKey) {
		log.trace("triggerResumed triggerKey:" + triggerKey);

	}

	@Override
	public void triggersResumed(String triggerGroup) {
		log.trace("triggersResumed triggerGroup:" + triggerGroup);

	}

	@Override
	public void jobAdded(JobDetail jobDetail) {
		log.trace("jobAdded jobDetail:" + jobDetail);

	}

	@Override
	public void jobDeleted(JobKey jobKey) {
		log.trace("jobDeleted jobKey:" + jobKey);

	}

	@Override
	public void jobPaused(JobKey jobKey) {
		log.trace("jobPaused jobKey:" + jobKey);

	}

	@Override
	public void jobsPaused(String jobGroup) {
		log.trace("jobsPaused jobGroup:" + jobGroup);

	}

	@Override
	public void jobResumed(JobKey jobKey) {
		log.trace("jobResumed jobKey:" + jobKey);

	}

	@Override
	public void jobsResumed(String jobGroup) {
		log.trace("jobsResumed jobGroup:" + jobGroup);

	}

	@Override
	public void schedulerError(String msg, SchedulerException cause) {
		log.error("schedulerError msg:" + msg, cause);

	}

	@Override
	public void schedulerInStandbyMode() {
		log.trace("schedulerInStandbyMode");

	}

	@Override
	public void schedulerStarted() {
		log.trace("schedulerStarted");

	}

	@Override
	public void schedulerStarting() {
		log.trace("schedulerStarting");

	}

	@Override
	public void schedulerShutdown() {
		log.trace("schedulerShutdown");

	}

	@Override
	public void schedulerShuttingdown() {
		log.trace("schedulerShuttingdown");

	}

	@Override
	public void schedulingDataCleared() {
		log.trace("schedulingDataCleared");

	}

}
