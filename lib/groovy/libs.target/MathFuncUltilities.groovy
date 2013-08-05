public class MathFuncUltilities{
	public static instances = new MathFuncUltilities()
	// Binding cdiv closure
	public static void doBindCdiv(binding) {
		instances.cdivf.delegate = binding
		binding.setVariable('cdivf', instances.cdivf)
		instances.cdivi.delegate = binding
		binding.setVariable('cdivi', instances.cdivi)
		instances.cdivl.delegate = binding
		binding.setVariable('cdivl', instances.cdivl)
	}

	// Binding simple diff closure
	public static void doBindSimpleDiff(binding) {
		instances.simple_diff.delegate = binding
		binding.setVariable('simple_diff', instances.simple_diff)
	}

	// Make diff(compare) for each diff_key
	def simple_diff = {diffKey->
		String prevOutputKey
		if (prevOUTPUT != null) {
		    diffKey.keySet().each{it->
		    	// Mapping record of OUTPUT and PrevOUTPUT based on GROUPKEYS
	    		// isMapped = true: mapped/ false: unmapped
		    	OUTPUT.each { rec ->
					prevOutputKey = ''
					for(groupkey in groupKeys){
						prevOutputKey += rec[groupkey]
					}
					def p_rec = prevOUTPUT[prevOutputKey]
					boolean isMapped = true
					for(groupkey in groupKeys){
						isMapped = isMapped && (rec[groupkey] == p_rec[groupkey])
					}
					if(isMapped){
						rec[diffKey[it]] = rec[it] - p_rec[it]
					}
				}
		    }
		}
	}

	// Get double value of division
	def cdivf = {dividee, divider, alternative_return_variable->
		def return_var
		if(divider != 0){
			def result = dividee/divider
			if(result <= Double.MAX_VALUE && result >= Double.MIN_VALUE){
				return_var = (dividee/divider).doubleValue()
			}else{
				return_var = alternative_return_variable
			}
		}else{
			return_var = alternative_return_variable
		}
		return return_var
	}

	// Get integer value of division
	def cdivi = {dividee, divider, alternative_return_variable->
		def return_var
		if(divider != 0){
			def result = dividee/divider
			if(result<=Integer.MAX_VALUE && result>=Integer.MIN_VALUE){
				return_var = (dividee/divider).intValue()
			}else{
				return_var = alternative_return_variable
			}
		}else{
			return_var = alternative_return_variable
		}
		return return_var
	}

	// Get long value of division
	def cdivl = {dividee, divider, alternative_return_variable->
		def return_var
		if(divider != 0){
			def result = dividee/divider
			if(result <= Long.MAX_VALUE && result >= Long.MIN_VALUE){
				return_var = (dividee/divider).longValue()
			}else{
				return_var = alternative_return_variable
			}
		}else{
			return_var = alternative_return_variable
		}
		return return_var
	}
}