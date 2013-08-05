import groovy.sql.Sql
	
public interface IDBConnectionSource{	
	public Sql newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params,logdir_params)
	public void savePassword(dbtype, instanceId, dbuser, passwd)
}