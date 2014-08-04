import javax.mail.*
import javax.mail.internet.*
import com.gmongo.GMongo
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class SendEmailPolicyJob implements Job{
	static final MONGO_COLLECTION = "policy_message"
	// Interval to get data from current time back to INTERVAL minutes
	static final INTERVAL_GETDATA = 1
	// Mail sender run after every INTERVAL_SENDER minutes
	static final INTERVAL_SENDER = 1
	def properties = MonitorJobConfigLoader.getProperties()
	static mapMongoDb = MongoDBConnection.getWiperdogConnection()
	static mongo
	static db
	public static void main(String[] args){
		try {
			//schedule the job
			SchedulerFactory schFactory = new StdSchedulerFactory();
			Scheduler sch = schFactory.getScheduler();
			
			// specify the job' s details..
			JobDetail job =	 JobBuilder.newJob(SendEmailPolicyJob.class)
									   .withIdentity("SendEmailPolicyJob")
				                       .storeDurably()
									   .build();
			sch.addJob(job, true);
			
			def	intervalRunSender = null
			if( SendEmailPolicyJob.properties.get(ResourceConstants.INTERVAL_SENDER) != null ) {
				intervalRunSender = Integer.valueOf((SendEmailPolicyJob.properties.get(ResourceConstants.INTERVAL_SENDER)))
			} else {
				intervalRunSender = SendEmailPolicyJob.INTERVAL_SENDER
			}
			// specify the running period of the job
			Trigger trigger = TriggerBuilder.newTrigger()
											.withSchedule(SimpleScheduleBuilder.simpleSchedule()
											.withIntervalInMinutes(intervalRunSender)
											.repeatForever())
											.build();
			TriggerBuilder<? extends Trigger> builder = trigger.getTriggerBuilder();
			Trigger newTrigger = builder.forJob(job).build();
			
			if (sch.getTrigger(trigger.getKey()) != null) {
				sch.rescheduleJob(trigger.getKey(), newTrigger);
			} else {
				sch.scheduleJob(newTrigger);
			}
      } catch (SchedulerException e) {
         e.printStackTrace();
      }
	}

  public void execute(JobExecutionContext jExeCtx) throws JobExecutionException {
		// TODO Auto-generated method stub
		try{

			if(mongo == null && mapMongoDb['gmongo'] != null) {
				mongo = mapMongoDb['gmongo']
			}
			if(db == null && mapMongoDb['db'] != null) {
				db = mapMongoDb['db']
			}

			if(db != null){
				def coll = db.getCollection(SendEmailPolicyJob.MONGO_COLLECTION)
				def message = ""
				def shell = new GroovyShell()
				def fetchAt = (Long)( new Date()).getTime()/1000
				def	intervalGetData = null
				if(properties.get(ResourceConstants.INTERVAL_GETDATA) != null) {
					intervalGetData = Integer.valueOf(properties.get(ResourceConstants.INTERVAL_GETDATA))
				} else {
					intervalGetData = SendEmailPolicyJob.INTERVAL_GETDATA
				}
				def lastestTime = (Long) ( new Date()).getTime()/1000 - intervalGetData*60
				def data = null
				if(fetchAt != null){
					data = coll.find(fetchedAt_bin: [ $gt: lastestTime,$lt: fetchAt ])
				} else {
					data = coll.find()
				}
				if(data.size() == 0) {
					println "No policy message generated since previous running !"
				} else {
					while(data.hasNext()){
						def rec = data.next()
						def recFetchAtBin = (Long)rec.fetchedAt_bin
						message += "Job name :" + rec['jobName'] + " - " + " messages: " + rec['message'] + "\n"
						if(fetchAt == null){
							fetchAt = recFetchAtBin
						} else {
							if(fetchAt < recFetchAtBin){
								fetchAt = recFetchAtBin
							}
						}
					}
					def fromMail = properties.get(ResourceConstants.FROM_EMAIL)
					def fromHost = properties.get(ResourceConstants.FROM_HOST)
					def fromPort = Integer.valueOf(properties.get(ResourceConstants.FROM_PORT))
					def fromPasswd = properties.get(ResourceConstants.FROM_PASSWD)
					def toMail = properties.get(ResourceConstants.TO_EMAIL)
					def emailContent = message
					def emailSubj = "Policy messages"
					def props = new Properties()
					props.put("mail.smtp.user", fromMail)
					props.put("mail.smtp.host", fromHost)
					props.put("mail.smtp.port", fromPort)
					props.put("mail.smtp.starttls.enable",true)
					props.put("mail.smtp.debug", true);
					props.put("mail.smtp.auth", true)
					props.put("mail.smtp.socketFactory.port", fromPort)
					props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
					props.put("mail.smtp.socketFactory.fallback", false)

					def auth = new SMTPAuthenticator()
					def session = Session.getInstance(props, auth)
					//session.setDebug(true);
					def msg = new MimeMessage(session)
					msg.setText(emailContent)
					msg.setSubject(emailSubj)
					msg.setFrom(new InternetAddress())
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(toMail))

					Transport transport = session.getTransport("smtps");
					transport.connect(fromHost, fromPort, fromMail, fromPasswd);
					transport.sendMessage(msg, msg.getAllRecipients());
					transport.close();
					println "Email has been sent to: " + toMail
				}
			} else{
				println "Send Policy Email: Cannot connect to MongoDB!"
			}
		}catch(AuthenticationFailedException ex){
			println "Can not log in to mail box ! Incorrect username or password !"
		}catch(Exception ex){
			println ex
		}
		return
	}

}
public class SMTPAuthenticator extends Authenticator {
	def properties = MonitorJobConfigLoader.getProperties()
	def fromMail = properties.get(ResourceConstants.FROM_EMAIL)
	def fromPasswd = properties.get(ResourceConstants.FROM_PASSWD)
	public PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(fromMail, fromPasswd);
	}
}