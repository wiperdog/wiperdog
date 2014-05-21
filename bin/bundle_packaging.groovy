class BundlePackaging{
	public static void main(String [] args){
		def resourcesDir = args[0]
		if(!(new File(resourcesDir).exists())){
			println "Resource directory not available !"
			return
		}
		def destination = args[1]
		def groupId = args[2]
		def artifactId = args[3]
		def version = args[4]
		
		def pom_template = args[5]
		
		def strPOMTemplate = pom_template
		def pomFile = new File("pom.xml")
		def strPOMFile = strPOMTemplate.replaceAll("@resourcesDir@",resourcesDir)
			strPOMFile = strPOMFile.replaceAll("@extractDestination@",destination).replaceAll("@groupid@",groupId).replaceAll("@artifactid@",artifactId).replaceAll("@version@",version)
		pomFile.setText(strPOMFile);		
		def mavenInstallCmd = "mvn clean install -f pom.xml"
		//excute maven install cmd
		StringBuffer output = new StringBuffer();
		Process p;
		try {
			
			p = Runtime.getRuntime().exec(mavenInstallCmd);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String line = "";
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		println output.toString()
		if(output.contains("[INFO] BUILD SUCCESS")){
			println "Bundle packaging successful at : target/" 
		} else {
			println "Bundle packaging failed !" 
		}
	}
}
