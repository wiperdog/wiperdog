import java.io.File;

interface JobDSLService {
	public boolean processJob(File jobfile);
	public boolean processCls(File clsfile);
	public boolean processTrigger(File trgfile);
	public boolean processInstances(File instfile);
}