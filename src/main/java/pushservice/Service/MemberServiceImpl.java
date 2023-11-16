package pushservice.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.api.core.ApiFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;

import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import pushservice.Application;
import pushservice.Enum.ResultCode;
import pushservice.Enum.TaskResultCode;
import pushservice.Enum.TaskState;
import pushservice.Handler.AimPushHandler;
import pushservice.Handler.AppHandler;
import pushservice.Handler.FirebaseHandler;
import pushservice.Handler.LineHandler;
import pushservice.Handler.PushHandler;
import pushservice.Handler.RegisterHandler;
import pushservice.Handler.SmtpHandler;
import pushservice.Pojo.AimNotifyMsgUser;
import pushservice.Pojo.AimUserMessage;
import pushservice.Pojo.PushResult;
import pushservice.Pojo.ReciverBag;
import pushservice.Pojo.RegisterResult;
import pushservice.Pojo.ResultResponse;
import pushservice.Pojo.TaskPojo;
import pushservice.Pojo.TaskResponse;
import pushservice.utils.CommonUtils;

@Service("MemberService")
public class MemberServiceImpl implements MemberService {

	public static final String PREFIX_MEMBERTOKEN = "mtoken";
	private static final String PREFIX_MEMBERLIST = "mlist";
	private static final String PREFIX_SCHEDULE_FIREBASE = "schedule:firebase";
	private static final String PREFIX_SCHEDULE_SMTP = "schedule:smtp";
	private static final String PREFIX_SCHEDULE_LINE = "schedule:line";
	private static final String PREFIX_TASK = "task";
	
	private Logger logger = Logger.getLogger(MemberServiceImpl.class);
	
	Pattern mamberIdRegex = Pattern.compile("^[a-zA-Z]\\w+$");
	private final static Pattern UUID_REGEX_PATTERN =
	        Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");
	
	@Autowired
    private RedisTemplate<String, Object> redis;
	
	@Autowired
	private RedisLockRegistry lockRegistry;
	
	@Autowired
	private AppService appService;
	
	@Value("${app.member-id-store:db}")
	String memberIdStore;
	
	@Autowired
	private MemberIdService memberIdService;
	
	@Value("${app.message_size:100}")
	int messageSize;
	
	@Value("${app.batch_size:400}")
	int batchSize;
	
	@Value("${app.batch_delay:1}")
	int batchDelay;
	
	@Override
	public List<RegisterResult> Register(RegisterHandler handler) {
		logger.trace("execute");
		List<RegisterResult> result = new ArrayList<RegisterResult>();
		if (StringUtils.isEmpty(handler.getApp())) return result;
		handler.getList().forEach((reg) -> {
			logger.info("會員資料");
			logger.info(reg.getToken());
			logger.info(handler.getApp());

			memberIdService.save(handler.getApp(), reg.getMemberId(), reg.getToken());
			
			result.add(new RegisterResult(reg, true));
		});
		return result;
	}

	/**
	 * Test method
	 */
	@Override
	public List<RegisterResult> GetByApp(String app) {

		List<RegisterResult> result = new ArrayList<RegisterResult>();

		String test = "";
		AppHandler appHandler = appService.getHandler(app);
		if (appHandler instanceof LineHandler)
			test = "Line";
		else if (appHandler instanceof FirebaseHandler)
			test = "Firebase";
		else if (appHandler instanceof SmtpHandler)
			test = "Smtp";
		test += " " + appHandler.getClass().getName();
		final String ah = test;
		
		String prefix = PREFIX_MEMBERTOKEN + ":" + app + ":";
		redis.keys(prefix + "*").forEach(k -> {
			RegisterResult reg = new RegisterResult();
			reg.memberId = k.substring(prefix.length());
			reg.result = true;
			reg.appHandler = ah;
			result.add(reg);
		});
		
		return result;
	}

	@Override
	public TaskResponse<PushResult> Push(PushHandler handler) throws InstantiationException, IllegalAccessException {
		logger.trace("execute");
		
		AppHandler appHandler = appService.getHandler(handler.getApp());
		if (appHandler == null || appHandler instanceof LineHandler) {
			return new TaskResponse<PushResult>(ResultCode.NotFound);
		}

		List<PushResult> result = new ArrayList<PushResult>();
		List<String> validated = new ArrayList<String>();
		logger.info(handler.isBroadcast());
		if (!handler.isBroadcast()) {

			// 驗證memberId目前是否存在
			handler.getMemberIdList().forEach(id -> {
				PushResult pr = new PushResult();
				logger.info(pr.toString());
				pr.memberId = id;
				pr.result = false;
				// memberId格式驗證
				if (mamberIdRegex.matcher(id).matches()) {
					// 驗證token存在
					if (memberIdService.exists(handler.getApp(), id)) {
						pr.result = true;
						validated.add(id);
					}
					validated.add(id);
				}
				result.add(pr);
			});
		}
		
		String serial = UUID.randomUUID().toString();
		
		// 不論驗證是否存在仍進入推撥
		if (handler.getMemberIdList().size() > 0 || handler.isBroadcast()) {
			if (!handler.isBroadcast()) {
				// meberIdList
				BoundListOperations<String, Object> opr = redis.boundListOps(PREFIX_MEMBERLIST + ":" + handler.getApp() + ":" + serial);
				handler.getMemberIdList().forEach(v -> opr.rightPush(v));
			}
			// task
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmss");
			String taskKey = PREFIX_TASK + ":" + sdf1.format(handler.getSchedule()) + ":" + sdf2.format(handler.getSchedule()) + ":" + serial;
			TaskPojo task = new TaskPojo();
			task.setApp(handler.getApp());
			task.setBroadcast(handler.isBroadcast());
			task.setBroadScan(ScanCursor.INITIAL.getCursor());
			task.setDataKey(serial);
			task.setTitle(handler.getTitle());
			task.setMessage(handler.getMessage());
			task.setSchedule(handler.getSchedule());
			task.setApp_type(handler.getApp_type());
			// default value
			task.setStatus(TaskState.Schedule);
			task.setResultCode(TaskResultCode.Empty);
			task.setTotal(handler.getMemberIdList().size());
			task.setSuccessCount(0);
			task.setErrorCount(0);
			// SAVE
			redis.boundValueOps(taskKey).set(task);
			if (appHandler instanceof FirebaseHandler)
				redis.boundZSetOps(PREFIX_SCHEDULE_FIREBASE).add(taskKey, handler.getSchedule().getTime());
			else redis.boundZSetOps(PREFIX_SCHEDULE_SMTP).add(taskKey, handler.getSchedule().getTime());
			
			//if (validated.size() == handler.getMemberIdList().size())
				return new TaskResponse<PushResult>(ResultCode.OK, serial, CommonUtils.cloneList(result, PushResult.class));
			//else return new TaskResponse<PushResult>(ResultCode.PartialContent, serial, result);
		}
		
		return new TaskResponse<PushResult>(ResultCode.NoContent, CommonUtils.cloneList(result, PushResult.class));
	}
	
	/**
	 * for Aim Line Handler (Line applications)
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@Override
	public TaskResponse<PushResult> AimPush(AimPushHandler handler) throws InstantiationException, IllegalAccessException {
		logger.trace("execute");
		
		AppHandler appHandler = appService.getHandler(handler.getApp());
		if (appHandler == null || !(appHandler instanceof LineHandler)) {
			return new TaskResponse<PushResult>(ResultCode.NotFound);
		} else if (handler.isBroadcast()) {
			return new TaskResponse<PushResult>(ResultCode.MethodNotAllowed);
		}
		List<PushResult> result = new ArrayList<PushResult>();
		List<AimUserMessage> validated = new ArrayList<AimUserMessage>();
		
		if (!handler.isBroadcast()) {

			// 驗證已存在token才會進task
			handler.getAimUserMessageList().forEach(aum -> {
				PushResult pr = new PushResult();
				pr.memberId = aum.getMemberId();
				pr.result = false;
				// memberId格式驗證
				//if (mamberIdRegex.matcher(pr.memberId).matches()) {
					validated.add(aum);
				//}
				result.add(pr);
			});
		}
		
		String serial = UUID.randomUUID().toString();
		// 允許部分token有效即建立task
		if (validated.size() > 0 || handler.isBroadcast()) {
			if (!handler.isBroadcast()) {
				// Receiver data
				BoundListOperations<String, Object> opr = redis.boundListOps(PREFIX_MEMBERLIST + ":" + handler.getApp() + ":" + serial);
				validated.forEach(v -> opr.rightPush(v));
			}
			// task
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat sdf2 = new SimpleDateFormat("HHmmss");
			String taskKey = PREFIX_TASK + ":" + sdf1.format(handler.getSchedule()) + ":" + sdf2.format(handler.getSchedule()) + ":" + serial;
			TaskPojo task = new TaskPojo();
			task.setApp(handler.getApp());
			task.setBroadcast(handler.isBroadcast());
			task.setBroadScan(ScanCursor.INITIAL.getCursor());
			task.setDataKey(serial);
			task.setTitle(handler.getTitle());
			task.setMessage(handler.getMessage());
			task.setSchedule(handler.getSchedule());
			// default value
			task.setStatus(TaskState.Schedule);
			task.setResultCode(TaskResultCode.Empty);
			task.setTotal(validated.size());
			task.setSuccessCount(0);
			task.setErrorCount(0);
			// SAVE
			redis.boundValueOps(taskKey).set(task);
			redis.boundZSetOps(PREFIX_SCHEDULE_LINE).add(taskKey, handler.getSchedule().getTime());
			
			if (validated.size() == handler.getAimUserMessageList().size())
				return new TaskResponse<PushResult>(ResultCode.OK, serial, CommonUtils.cloneList(result, PushResult.class));
			else return new TaskResponse<PushResult>(ResultCode.PartialContent, serial, CommonUtils.cloneList(result, PushResult.class));
		}
		
		return new TaskResponse<PushResult>(ResultCode.NoContent, CommonUtils.cloneList(result, PushResult.class));
	}
	
	@Override
	public boolean Cancel(String serial) {
		if (!UUID_REGEX_PATTERN.matcher(serial).matches()) return false;
		Set<String> keys = redis.keys(PREFIX_TASK + ":*:" + serial);
		List<String> result = new ArrayList<>();
		keys.forEach(k -> {
			BoundValueOperations<String, Object> taskOps = redis.boundValueOps(k);
			TaskPojo task = (TaskPojo)taskOps.get();
			if (task != null) {
				if (task.getStatus() == TaskState.Schedule || task.getStatus() == TaskState.Inprocess) {
					task.setStatus(TaskState.Cancel);
					taskOps.set(task);
					result.add(k);
				}
			}
		});
		
		return result.size() > 0;
	}
	
	/**
	 * Firebase主要任務處理序
	 * Redis:
	 *   KEY: PREFIX_SCHEDULE, 任務排程時間;，VALUE: Task Key;
	 *   KEY: Task Key; VALUE: {@link TaskPojo};
	 * @param consumerKey for trace consumer
	 */
	@Override
	public boolean FirebaseTaskConsumer(String consumerKey) {
		String lockstr = "FirebaseTaskConsumer";
		Lock lock = lockRegistry.obtain(lockstr);
		boolean locked = false;
		try {
			locked = lock.tryLock();
			if (!locked) {
				return false;
			}
			// 
			logger.trace("TaskConsummer execute: " + consumerKey);
			double timenow = new Date().getTime();
			logger.trace("Search task from 0 to " + Double.valueOf(timenow).longValue());
			// WARNING! 如果Redis內取得任何資料結構不正確，會導致exception
			BoundZSetOperations<String, Object> scheduleOps = redis.boundZSetOps(PREFIX_SCHEDULE_FIREBASE);
			Set<ZSetOperations.TypedTuple<Object>> taskList = scheduleOps.rangeByScoreWithScores(0, timenow);
			if (taskList.size() > 0) {
				logger.info("Task waiting: " + taskList.size());

				taskList.forEach(t -> {
					String taskKey = (String)t.getValue();
					BoundValueOperations<String, Object> taskOps = redis.boundValueOps(taskKey);
					Object taskObj = taskOps.get();
					if (taskObj == null) {
						logger.error("Schdule value is null: " + taskKey);
						scheduleOps.remove(taskKey);
						return;
					}
					TaskPojo task = (TaskPojo)taskObj;
					// check task status
					if (task.getStatus() != TaskState.Schedule && task.getStatus() != TaskState.Inprocess) {
						logger.warn("Task isn't right status:" + taskKey + ":" + task.getStatus().getValue());
						scheduleOps.remove(taskKey);
						return;
					}
					
					AppHandler appHandler = appService.getHandler(task.getApp());
					if (appHandler == null) {
						logger.error("Cannot find app handler for " + task.getApp());
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					} else if (!(appHandler instanceof FirebaseHandler)) {
						logger.error(task.getApp() + " is not a firebase app.");
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					}
					
					// task info
					logger.info("Task: " + task.getDataKey());
					logger.info("IsBroadcast: " + task.isBroadcast());
					Date date = new Date(Double.valueOf(t.getScore()).longValue());
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					logger.info("Schedule: " + sdf.format(date));
					if (task.isBroadcast() && task.getStatus() == TaskState.Schedule) {
						task.setBroadScan(ScanCursor.INITIAL.getCursor());
					}
					task.setStatus(TaskState.Inprocess);

					// pop all member id
					List<ReciverBag> list = new ArrayList<ReciverBag>();
					// prepare for memberList
					String dataKey = PREFIX_MEMBERLIST + ":" + task.getApp() + ":" + task.getDataKey();
					// prepare for broadcast
					String scanKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":*";
					// ScanOptions scanOptions = ScanOptions.scanOptions().match(scanKey).count(messageSize).build();
					ScanArgs scanArgs = ScanArgs.Builder.limit(messageSize).match(scanKey);
					Queue<String> scanList = new LinkedList<String>();
					RedisConnection redisConnection = null;
					ScanCursor scanCursor = null;
					RedisKeyAsyncCommands<String, String> commands = null;
					
					try {
						if (task.isBroadcast()) {
							// lettuce scan by cursor
							redisConnection = redis.getConnectionFactory().getConnection();
							scanCursor = ScanCursor.of(task.getBroadScan());
							commands = (RedisKeyAsyncCommands<String, String>)redisConnection.getNativeConnection();
						} else {
							redis.boundListOps(dataKey);
						}
	
						int send = 0;
						do {
							list.clear();
							// prepare message list in a batch
							while (list.size() < messageSize) {
								String memberId = "";
								
								if (task.isBroadcast()) {
									// scan next if list empty
									while (scanList.size() == 0 && !scanCursor.isFinished()) {
										// scanCursor = ScanCursor.of(task.getBroadScan());
										KeyScanCursor<byte[]> keyScanCursor = (KeyScanCursor) commands.scan(scanCursor, scanArgs).get();
										if (keyScanCursor != null) {
											keyScanCursor.getKeys().forEach(b -> scanList.add(new String(b).substring(scanKey.length() - 1)));
											task.setBroadScan(keyScanCursor.getCursor());
											scanCursor = keyScanCursor;
										} else {
											scanCursor = ScanCursor.FINISHED;
										}
									}
									if (scanList.size() > 0) {
										memberId = scanList.poll();
									}
								} else {
									memberId = (String)redis.boundListOps(dataKey).leftPop();
									logger.trace("pop from list" + dataKey + ": " + memberId);
								}
								// Until no memberId
								if (StringUtils.isEmpty(memberId)) break;
								// get member's token
								
								String tokenKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":" + memberId;
								String token = memberIdService.getContent(task.getApp(), memberId);
								if (StringUtils.isEmpty(token)) {
									logger.warn("Member's token missing: " + tokenKey);
									task.setErrorCount(task.getErrorCount() + 1);
									continue;
								}
								//截斷過長訊息
								if (task.getMessage().length() > 100) {
									task.setMessage(task.getMessage().substring(0, 100));
								}

								//確認黑名單
								String categoryFromPush =  task.getApp_type().get("category").toString();
								if (categoryFromPush == null) {
									categoryFromPush = "";
								}
								categoryFromPush = categoryFromPush.replace("\"", "");
								Object oCate = redis.opsForValue().get("mswitch:"+task.getApp()+":"+memberId);
								String cate = Optional.ofNullable(oCate)
								        .map(obj -> (obj instanceof String) ? (String) obj : null)
								        .orElse("");
								
								String[] strArr = cate.split(",");
								logger.info(cate, null);
								boolean isSend = true;
								for (String s : strArr) {
									logger.info(categoryFromPush);
									if (s.equals(categoryFromPush)) {
										isSend = false;
									}
								}
								if (isSend) {
									ReciverBag bag = new ReciverBag();
									bag.setToken(token);
									bag.setMemberId(memberId);
									list.add(bag);
								}
								
							}
							
							if (list.size() > 0) {
								logger.trace("Sending message: " + list.size());
								redis.expire("redis-lock:" + lockstr, 10, TimeUnit.MINUTES);
								// BatchSend(task, list);
								
								appHandler.sendMessage(task, list);
								taskOps.set(task);
								send++;
								TimeUnit.SECONDS.sleep(1);
								// delay at reach batch size
								if (send >= batchSize) {
									try {
										TimeUnit.SECONDS.sleep(batchDelay);
									} catch (InterruptedException e) {
										logger.error("InterruptedException: " + consumerKey, e);
									}
									send = 0;
								}
							}
						} while (list.size() == messageSize);
					} catch (InterruptedException | ExecutionException e) {
						logger.error("task failure: " + task.getDataKey(), e);
					} finally {
						// TODO 確認是否需要關閉
						// if (redisConnection != null)
						//	redisConnection.close();
					}
					// Finish
					task.setStatus(TaskState.Finish);
					taskOps.set(task);
					scheduleOps.remove(taskKey);
					logger.info("Task finish:" + taskKey);
				});
			}
			return true;
		/*} catch (Exception e) {
			logger.error("ex: ", e);
			return false;
		*/
		} finally {
			if (locked) {
				logger.trace("TaskConsummer finish: " + consumerKey);
				lock.unlock();
			}
		}
	}
	
	/**
	 * Smtp主要任務處理序
	 * TODO 尚未調整流程
	 * Redis:
	 *   KEY: PREFIX_SCHEDULE, 任務排程時間;，VALUE: Task Key;
	 *   KEY: Task Key; VALUE: {@link TaskPojo};
	 * @param consumerKey for trace consumer
	 */
	@Override
	public boolean SmtpTaskConsumer(String consumerKey) {
		String lockstr = "SmtpTaskConsumer";
		Lock lock = lockRegistry.obtain(lockstr);
		boolean locked = false;
		try {
			locked = lock.tryLock();
			if (!locked) {
				return false;
			}
			// 
			logger.trace("TaskConsummer execute: " + consumerKey);
			double timenow = new Date().getTime();
			logger.trace("Search task from 0 to " + Double.valueOf(timenow).longValue());
			// WARNING! 如果Redis內取得任何資料結構不正確，會導致exception
			BoundZSetOperations<String, Object> scheduleOps = redis.boundZSetOps(PREFIX_SCHEDULE_SMTP);
			Set<ZSetOperations.TypedTuple<Object>> taskList = scheduleOps.rangeByScoreWithScores(0, timenow);
			if (taskList.size() > 0) {
				logger.info("Task waiting: " + taskList.size());
								
				taskList.forEach(t -> {
					String taskKey = (String)t.getValue();
					BoundValueOperations<String, Object> taskOps = redis.boundValueOps(taskKey);
					Object taskObj = taskOps.get();
					if (taskObj == null) {
						logger.error("Schdule value is null: " + taskKey);
						scheduleOps.remove(taskKey);
						return;
					}
					TaskPojo task = (TaskPojo)taskObj;
					// check task status
					if (task.getStatus() != TaskState.Schedule && task.getStatus() != TaskState.Inprocess) {
						logger.warn("Task isn't right status:" + taskKey + ":" + task.getStatus().getValue());
						scheduleOps.remove(taskKey);
						return;
					}
					
					AppHandler appHandler = appService.getHandler(task.getApp());
					if (appHandler == null) {
						logger.error("Cannot find app handler for " + task.getApp());
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					} else if (!(appHandler instanceof LineHandler)) {
						logger.error(task.getApp() + " is not a smtp app.");
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					}
					
					// task info
					logger.info("Task: " + task.getDataKey());
					logger.info("IsBroadcast: " + task.isBroadcast());
					Date date = new Date(Double.valueOf(t.getScore()).longValue());
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					logger.info("Schedule: " + sdf.format(date));
					if (task.isBroadcast() && task.getStatus() == TaskState.Schedule) {
						task.setBroadScan(ScanCursor.INITIAL.getCursor());
					}
					task.setStatus(TaskState.Inprocess);

					
					// pop all member id
					List<ReciverBag> list = new ArrayList<ReciverBag>();
					// prepare for memberList
					String dataKey = PREFIX_MEMBERLIST + ":" + task.getApp() + ":" + task.getDataKey();
					// prepare for broadcast
					String scanKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":*";
					// ScanOptions scanOptions = ScanOptions.scanOptions().match(scanKey).count(messageSize).build();
					ScanArgs scanArgs = ScanArgs.Builder.limit(messageSize).match(scanKey);
					Queue<String> scanList = new LinkedList<String>();
					RedisConnection redisConnection = null;
					ScanCursor scanCursor = null;
					RedisKeyAsyncCommands<String, String> commands = null;
					
					try {
						if (task.isBroadcast()) {
							// lettuce scan by cursor
							redisConnection = redis.getConnectionFactory().getConnection();
							scanCursor = ScanCursor.of(task.getBroadScan());
							commands = (RedisKeyAsyncCommands<String, String>)redisConnection.getNativeConnection();
						} else {
							redis.boundListOps(dataKey);
						}
	
						int send = 0;
						do {
							list.clear();
							// prepare message list in a batch
							while (list.size() < messageSize) {
								String memberId = "";
								
								if (task.isBroadcast()) {
									// scan next if list empty
									while (scanList.size() == 0 && !scanCursor.isFinished()) {
										// scanCursor = ScanCursor.of(task.getBroadScan());
										KeyScanCursor<byte[]> keyScanCursor = (KeyScanCursor) commands.scan(scanCursor, scanArgs).get();
										if (keyScanCursor != null) {
											keyScanCursor.getKeys().forEach(b -> scanList.add(new String(b).substring(scanKey.length() - 1)));
											task.setBroadScan(keyScanCursor.getCursor());
											scanCursor = keyScanCursor;
										} else {
											scanCursor = ScanCursor.FINISHED;
										}
									}
									if (scanList.size() > 0) {
										memberId = scanList.poll();
									}
								} else {
									memberId = (String)redis.boundListOps(dataKey).leftPop();
									logger.trace("pop from list" + dataKey + ": " + memberId);
								}
								// Until no memberId
								if (StringUtils.isEmpty(memberId)) break;
								// get member's token
								String tokenKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":" + memberId;
								String token = memberIdService.getContent(task.getApp(), memberId);
								if (StringUtils.isEmpty(token)) {
									logger.warn("Member's token missing: " + tokenKey);
									task.setErrorCount(task.getErrorCount() + 1);
									continue;
								}
								ReciverBag bag = new ReciverBag();
								bag.setToken(token);
								bag.setMemberId(memberId);
								list.add(bag);
							}
							
							if (list.size() > 0) {
								logger.trace("Sending message: " + list.size());
								redis.expire("redis-lock:" + lockstr, 10, TimeUnit.MINUTES);
								// BatchSend(task, list);
								appHandler.sendMessage(task, list);
								taskOps.set(task);
								send++;
								TimeUnit.SECONDS.sleep(1);
								// delay at reach batch size
								if (send >= batchSize) {
									try {
										TimeUnit.SECONDS.sleep(batchDelay);
									} catch (InterruptedException e) {
										logger.error("InterruptedException: " + consumerKey, e);
									}
									send = 0;
								}
							}
						} while (list.size() == messageSize);
					} catch (InterruptedException | ExecutionException e) {
						logger.error("task failure: " + task.getDataKey(), e);
					} finally {
						// TODO 確認是否需要關閉
						// if (redisConnection != null)
						//	redisConnection.close();
					}
					// Finish
					task.setStatus(TaskState.Finish);
					taskOps.set(task);
					scheduleOps.remove(taskKey);
					logger.info("Task finish:" + taskKey);
				});
			}
			return true;
		/*} catch (Exception e) {
			logger.error("ex: ", e);
			return false;
			*/
		} finally {
			if (locked) {
				logger.trace("TaskConsummer finish: " + consumerKey);
				lock.unlock();
			}
		}
	}
		
	/**
	 * Line主要任務處理序
	 * 不支援broadcast
	 * TODO 尚未調整流程
	 * Redis:
	 *   KEY: PREFIX_SCHEDULE, 任務排程時間;，VALUE: Task Key;
	 *   KEY: Task Key; VALUE: {@link TaskPojo};
	 * @param consumerKey for trace consumer
	 */
	@Override
	public boolean LineTaskConsumer(String consumerKey) {
		String lockstr = "LineTaskConsumer";
		Lock lock = lockRegistry.obtain(lockstr);
		boolean locked = false;
		try {
			locked = lock.tryLock();
			if (!locked) {
				return false;
			}
			// 
			logger.trace("TaskConsummer execute: " + consumerKey);
			double timenow = new Date().getTime();
			logger.trace("Search task from 0 to " + Double.valueOf(timenow).longValue());
			// WARNING! 如果Redis內取得任何資料結構不正確，會導致exception
			BoundZSetOperations<String, Object> scheduleOps = redis.boundZSetOps(PREFIX_SCHEDULE_LINE);
			Set<ZSetOperations.TypedTuple<Object>> taskList = scheduleOps.rangeByScoreWithScores(0, timenow);
			if (taskList.size() > 0) {
				logger.info("Task waiting: " + taskList.size());
								
				taskList.forEach(t -> {
					String taskKey = (String)t.getValue();
					BoundValueOperations<String, Object> taskOps = redis.boundValueOps(taskKey);
					Object taskObj = taskOps.get();
					if (taskObj == null) {
						logger.error("Schdule value is null: " + taskKey);
						scheduleOps.remove(taskKey);
						return;
					}
					TaskPojo task = (TaskPojo)taskObj;
					// check task status
					if (task.getStatus() != TaskState.Schedule && task.getStatus() != TaskState.Inprocess) {
						logger.warn("Task isn't right status:" + taskKey + ":" + task.getStatus().getValue());
						scheduleOps.remove(taskKey);
						return;
					}
					
					AppHandler appHandler = appService.getHandler(task.getApp());
					if (appHandler == null) {
						logger.error("Cannot find app handler for " + task.getApp());
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					} else if (!(appHandler instanceof LineHandler)) {
						logger.error(task.getApp() + " is not a line app.");
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					} else if (task.isBroadcast()) {
						logger.error(task.getApp() + " not support broadcast.");
						scheduleOps.remove(taskKey);
						task.setStatus(TaskState.Error);
						taskOps.set(task);
						return;
					}
					
					// task info
					logger.info("Task: " + task.getDataKey());
					Date date = new Date(Double.valueOf(t.getScore()).longValue());
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					logger.info("Schedule: " + sdf.format(date));
					task.setStatus(TaskState.Inprocess);
					
					// pop all member id
					List<AimNotifyMsgUser> list = new ArrayList<AimNotifyMsgUser>();
					// prepare for memberList
					String dataKey = PREFIX_MEMBERLIST + ":" + task.getApp() + ":" + task.getDataKey();
					// prepare for broadcast
					String scanKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":*";
					ScanArgs.Builder.limit(messageSize).match(scanKey);
					new LinkedList<String>();
					try {
						redis.boundListOps(dataKey);
	
						int send = 0;
						do {
							list.clear();
							// prepare message list in a batch
							while (list.size() < messageSize) {
								AimUserMessage reciver = null;
								
								reciver = (AimUserMessage)redis.boundListOps(dataKey).leftPop();
								logger.trace("pop from list" + dataKey + ": " + reciver);
								
								// Until no memberId
								if (reciver ==  null) break;
								// get member's token
								/*
								String tokenKey = PREFIX_MEMBERTOKEN + ":" + task.getApp() + ":" + reciver.getMemberId();
								String token = (String)redis.opsForValue().get(tokenKey);
								if (StringUtils.isEmpty(token)) {
									logger.warn("Member's token missing: " + tokenKey);
									task.setErrorCount(task.getErrorCount() + 1);
									continue;
								}
								*/
								AimNotifyMsgUser user = new AimNotifyMsgUser();
								user.setTo(reciver.getMemberId()); // 南山直接給line id or電話號碼
								user.setParams(reciver.getParams());
								list.add(user);
							}
							
							if (list.size() > 0) {
								logger.trace("Sending message: " + list.size());
								redis.expire("redis-lock:" + lockstr, 10, TimeUnit.MINUTES);
								// BatchSend(task, list);
								((LineHandler)appHandler).sendAimMessage(task, list);
								taskOps.set(task);
								send++;
								//TimeUnit.SECONDS.sleep(1);
								// delay at reach batch size
								if (send >= batchSize) {
									try {
										TimeUnit.SECONDS.sleep(batchDelay);
									} catch (InterruptedException e) {
										logger.error("InterruptedException: " + consumerKey, e);
									}
									send = 0;
								}
							}
						} while (list.size() == messageSize);
					} finally {
						// TODO 確認是否需要關閉
						// if (redisConnection != null)
						//	redisConnection.close();
					}
					// Finish
					task.setStatus(TaskState.Finish);
					taskOps.set(task);
					scheduleOps.remove(taskKey);
					logger.info("Task finish:" + taskKey);
				});
			}
			return true;
		/*} catch (Exception e) {
			logger.error("ex: ", e);
			return false;
			*/
		} finally {
			if (locked) {
				logger.trace("TaskConsummer finish: " + consumerKey);
				lock.unlock();
			}
		}
	}
}
