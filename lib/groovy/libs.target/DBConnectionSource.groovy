interface DBConnectionSource {
	def newSqlInstance(dbInfo, datadir_params, dbversion_params, programdir_params,logdir_params)
	def closeConnection()
	def getConnection()
	def getBinding()
}
