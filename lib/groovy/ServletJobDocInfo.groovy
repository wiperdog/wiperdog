import java.text.BreakIterator
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.codehaus.groovy.antlr.GroovySourceAST
import org.codehaus.groovy.antlr.LineColumn
import org.codehaus.groovy.antlr.SourceBuffer;
import org.codehaus.groovy.antlr.UnicodeEscapingReader
import org.codehaus.groovy.antlr.parser.GroovyLexer
import org.codehaus.groovy.antlr.parser.GroovyRecognizer;
import org.codehaus.groovy.antlr.treewalker.SourceCodeTraversal
import org.codehaus.groovy.antlr.treewalker.Visitor
import org.codehaus.groovy.antlr.treewalker.VisitorAdapter
import org.codehaus.groovy.antlr.AntlrASTProcessor;
import org.codehaus.groovy.groovydoc.GroovyTag
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyTag

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import groovy.json.*

public class JobDocInformation extends HttpServlet {

	static final String JOB_DIR = MonitorJobConfigLoader.getProperties().get(ResourceConstants.JOB_DIRECTORY)
	static final String HOMEPATH = System.getProperty("felix.home")
	static final String PARAMFILE = "var/conf/default.params"
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			resp.setContentType("json")
			resp.addHeader("Access-Control-Allow-Origin", "*")
			PrintWriter out = resp.getWriter()
			def data2CreateMenu = GenerateTreeMenu.getData2CreateMenu(JOB_DIR)
			def treeItem = GenerateTreeMenu.getMenuItemsStr(data2CreateMenu['root'], data2CreateMenu['output'])
			def builderListJob = new JsonBuilder(treeItem)
			out.println(builderListJob.toPrettyString());
		} catch(Exception ex) {
			println "ERROR DOGET: " + ex
			ex.printStackTrace()
		}
	}

	@Override
	void doPut(HttpServletRequest req, HttpServletResponse resp) {
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		def contentText = req.getInputStream().getText()
	}
	
	@Override
	void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/html")
		resp.setContentType("json")
		resp.addHeader("Access-Control-Allow-Origin", "*")
		PrintWriter out = resp.getWriter()
		
		try {
			// Get default params
			def paramFile = new File(HOMEPATH, PARAMFILE)
			def params = new Params(paramFile)
			
			// Get job
			def contentText = req.getInputStream().getText()
			def slurper = new JsonSlurper()
			def object = slurper.parseText(contentText)
			def jobName = object.job
			def jobPath = JOB_DIR +"/"+ jobName
			def jobFile = new File(jobPath)

			// Get jobdoc info
			def jobDocInfo = new Job(jobFile, params)
			
			// Set to data for Response
			def jobDocRet = [:]
			jobDocRet["jobName"] = jobDocInfo.getJobName()
			jobDocRet["description"] = jobDocInfo.getDescriptions()
			jobDocRet["targetVersion"] = jobDocInfo.getTargetVersion()
			jobDocRet["param"] = jobDocInfo.getParams()
			jobDocRet["return"] = jobDocInfo.getReturn()
			jobDocRet["returnParams"] = jobDocInfo.getReturnParams()
			
			// Return data
			def info = new JsonBuilder(jobDocRet)
			out.print(info.toString());
		} catch (Exception ex) {
			println "ERROR DOPOST: " + ex
			def message = [status:"failed"]
			def builder = new JsonBuilder(message)
			out.print(builder.toPrettyString())
		}
	}
}

def JobDocInfo
try {
	JobDocInfo = new JobDocInformation()
} catch (e) {
	println e
}

if (JobDocInfo != null) {
	def props = new java.util.Hashtable()
	props.put "alias", "/JobDocInfoServlet"
	// safeRegisterService method is customized version of BundleContext.registerService()
	ctx.safeRegisterService("javax.servlet.Servlet", JobDocInfo, props)
}

class Job {
	def jobParser
	def jobScript
	
	Job(file, params) {
		jobParser = new JobParser(file)
		jobScript = new JobScript(file, params)
	}
	
	def getJobName() {	return jobScript.getJobName() }
	def getMonitoringType() { return jobScript.getMonitoringType() }
	def getDbType() { return jobScript.getDbType() }
	def getResourceId() { return jobScript.getResourceId() }
	def getSendType() { return jobScript.getSendType() }
	def getItems()  { return jobScript.getItems() }
	def getSequence() { return jobScript.getSequence() }
	def getQuery() { return jobScript.getQuery() }
	def getFetchAction() { return jobScript.getFetchAction() }
	def getAccumulate() { return jobScript.getAccumulate() }
	def getDescriptions() { return jobParser.getDescriptions() }
	def getFirstSentenceDescription() { return jobParser.getFirstSentenceDescription() }
	def getTargetVersion() { return jobParser.getTargetVersion() }
	def getParams() { return jobParser.getParams() }
	def getReturn() { return jobParser.getReturn() }
	def getReturnParams() { return jobParser.getReturnParams() }
}

class JobParser {
	
	def comments = []
	def firstSentenceComment = null
	def tags = []
	
	JobParser(file) {
		def src = file.text
		def rawComments = parseGroovy(src)
		rawComments.each {
			def comment = calculateComment(it)
			if(comment != null) {
				comments.add(comment);
			}
			if(firstSentenceComment == null) {
				firstSentenceComment = calculateFirstSentence(it)
			}
			def tags = calculateTags(it)
			if(tags != null) {
				this.tags.addAll(tags)
			}
			
		}
	}
	
	def parseGroovy(src) {
		SourceBuffer sourceBuffer = new SourceBuffer();
		GroovyRecognizer parser = getGroovyParser(src, sourceBuffer);
		parser.compilationUnit();
		def ast = parser.getAST();
		Visitor visitor = new JobVisitor(sourceBuffer);
		AntlrASTProcessor traverser = new SourceCodeTraversal(visitor);
		traverser.process(ast);
		return ((JobVisitor) visitor).getComments()
	}
	
	def GroovyRecognizer getGroovyParser(input, sourceBuffer) {
		UnicodeEscapingReader unicodeReader = new UnicodeEscapingReader(new StringReader(input), sourceBuffer);
		GroovyLexer lexer = new GroovyLexer(unicodeReader);
		unicodeReader.setLexer(lexer);
		GroovyRecognizer parser = GroovyRecognizer.make(lexer);
		parser.setSourceBuffer(sourceBuffer);
		return parser;
	}
	
	def getDescriptions() {
		return comments
	}
	
	def getFirstSentenceDescription() {
		return firstSentenceComment
	}
	
	def getTargetVersion() {
		def tag = tags.find { it.name == "targetVersion" }
		if(tag != null) {
			return tag.text
		}
		return null
	}
	
	def getParams() {
		def tags = this.tags.findAll { it.name == "param" }
		return tags.collectEntries {
			[(it.param):it.text]
		}
	}
	
	def getReturn() {
		def tag = tags.find { it.name == "return" }
		if(tag != null) {
			return tag.text
		}
		return null
	}

	def getReturnParams() {
		def tags = this.tags.findAll { it.name == "returnParam" }
		return tags.collectEntries {
			[(it.param):it.text]
		}
	}

	
	def Pattern TAG2_PATTERN = Pattern.compile("(?s)([a-zA-Z]+)\\s+(.*)");
	def Pattern TAG3_PATTERN = Pattern.compile("(?s)([a-zA-Z]+)\\s+(\\S*)\\s+(.*)");
	
	def calculateTags(rawCommentText) {
		String trimmed = rawCommentText.replaceFirst("(?s).*?\\*\\s*@", "@")
		String cleaned = trimmed.replaceAll('(?m)^\\s*\\*\\s*([^*]*)$', '$1').trim()
		String[] split = cleaned.split("(?m)^@");
		List<GroovyTag> result = new ArrayList<GroovyTag>();
		for (String s : split) {
			String tagname = null;
			if (s.startsWith("param") || s.startsWith("throws") || s.startsWith("returnParam")) {
				Matcher m = TAG3_PATTERN.matcher(s);
				if (m.find()) {
					tagname = m.group(1);
					result.add(new SimpleGroovyTag(tagname, m.group(2), m.group(3)));
				}
			} else {
				Matcher m = TAG2_PATTERN.matcher(s);
				if (m.find()) {
					tagname = m.group(1);
					result.add(new SimpleGroovyTag(tagname, null, m.group(2)));
				}
			}
			if ("deprecated".equals(tagname)) {
				setDeprecated(true);
			}
		}
		return result
	}
	
	def calculateComment(String rawCommentText) {
		// remove all the * from beginning of lines
		String text = rawCommentText.replaceAll("(?m)^\\s*\\*", "").trim();
		// assume @tag signifies end of sentence
		text = text.replaceFirst("(?ms)\\n\\s*@(see|param|throws|return|author|since|exception|version|deprecated|todo|returnParam|targetVersion)\\s.*", "").trim();
		return text
	}
	
	public static String calculateFirstSentence(String rawCommentText) {
		// remove all the * from beginning of lines
		String text = rawCommentText.replaceAll("(?m)^\\s*\\*", "").trim();
		// assume a <p> paragraph tag signifies end of sentence
		text = text.replaceFirst("(?ms)<p>.*", "").trim();
		// assume completely blank line signifies end of sentence
		text = text.replaceFirst("(?ms)\\n\\s*\\n.*", "").trim();
		// assume @tag signifies end of sentence
		text = text.replaceFirst("(?ms)\\n\\s*@(see|param|throws|return|author|since|exception|version|deprecated|todo|returnParam|targetVersion)\\s.*", "").trim();
		// Comment Summary using first sentence (Locale sensitive)
		BreakIterator boundary = BreakIterator.getSentenceInstance(Locale.getDefault()); // todo - allow locale to be passed in
		boundary.setText(text);
		int start = boundary.first();
		int end = boundary.next();
		if (start > -1 && end > -1) {
			// need to abbreviate this comment for the summary
			text = text.substring(start, end);
		}
		return text;
	}
	
	class JobVisitor extends VisitorAdapter {
		
		def Pattern PREV_JAVADOC_COMMENT_PATTERN = Pattern.compile("(?s)/\\*\\*(.*?)\\*/");
		
		def sourceBuffer
		def lastLineCol = new LineColumn(1, 1)
		def comments = []
		
		JobVisitor(sourceBuffer) {
			this.sourceBuffer = sourceBuffer
		}

		@Override
		public void visitAssign(GroovySourceAST t, int visit) {
			def comment = getJavaDocCommentsBeforeNode(t, visit)
			if(comment != null) {
				comments.add(comment)
			}
		}

		def getJavaDocCommentsBeforeNode(GroovySourceAST t, int visit) {
			if (visit != OPENING_VISIT) {
				return null
			}
			def result = null
			LineColumn thisLineCol = new LineColumn(t.getLine(), t.getColumn());
			String text = sourceBuffer.getSnippet(lastLineCol, thisLineCol);
			if (text != null) {
				Matcher m = PREV_JAVADOC_COMMENT_PATTERN.matcher(text);
				if (m.find()) {
					result = m.group(1);
				}
			}
			lastLineCol = thisLineCol;
			return result
		}
	}
}
	
class JobScript {
	def binding;

	JobScript(file, params) {
		def shell = new GroovyShell()
		def script = shell.parse(file)
		this.binding = script.getBinding()
		this.binding.setVariable("parameters", params.get())
		script.run()
	}

	def getBindingData(key) {
		if(this.binding.hasVariable(key)) {
			return this.binding.getVariable(key)
		}
		return null
	}

	def getJobName() {
		def jobData = getBindingData("JOB")
		if(jobData == null) {
			return null;
		}
		return jobData.name
	}

	def getMonitoringType() { return getBindingData("MONITORINGTYPE") }

	def getDbType() { return getBindingData("DBTYPE") }

	def getResourceId() { return getBindingData("RESOURCEID") }

	def getSendType() { return getBindingData("SENDTYPE") }

	def getItems() {
		def keyexprData = getBindingData("KEYEXPR")
		if(keyexprData == null) {
			return null
		}
		if(keyexprData instanceof List) {
			return keyexprData.toString();
		} else if(keyexprData instanceof Map) {
			if(keyexprData.containsKey("_root")) {
				return keyexprData._root.toString();
			}
		}
		return null
	}

	def getSequence(binding) {
		def keyexprData = getBindingData("KEYEXPR")
		if(keyexprData == null) {
			return null
		}
		if(keyexprData instanceof Map) {
			if(keyexprData.containsKey("_sequence")) {
				return keyexprData._sequence.toString();
			}
		}
		return null
	}
	
	def getQuery() { return getBindingData("QUERY") }
	
	def getFetchAction() { return getBindingData("FETCHACTION") }
	
	def getAccumulate() { return getBindingData("ACCUMULATE") }
}
class Params {
	def paramFile
	
	Params(paramFile) {
		this.paramFile = paramFile
	}
	def get() {
		def ret
		def shell = new GroovyShell()
		if (paramFile != null) {
			ret = shell.evaluate(paramFile)
		} 
		return ret
	}	
}