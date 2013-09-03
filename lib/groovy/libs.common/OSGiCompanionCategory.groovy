import org.osgi.framework.BundleContext

/**
 * ctx (BundleContext) を少し楽に使えるようにCategoryを作成
 */
class OSGiCompanionCategory {
	static def registMap = [:]
	static def safeRegisterService(BundleContext self, String name, Object service, properties) {
		println registMap
		// def key = [name: name, props: properties]
		def key = service.class.getName()
		def reg = registMap[key]
		if (reg != null) {
			reg.unregister()
		}
		reg = self.registerService(name, service, properties)
		registMap[key] = reg
		return reg
	}
}

