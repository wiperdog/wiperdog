
class JobControlException extends Exception{
	String jobName
	String message
	
	public JobControlException(){
		super()
	}
	
	public JobControlException(String jobName,String message){
		this.jobName = jobName
		this.message = message
	}
	public String getExceptionMessage(){
		return jobName + " : " + message 
	}
}
