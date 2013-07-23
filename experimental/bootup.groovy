import groovy.grape.Grape
import java.util.Map

@Grab(group='org.apache.felix', module= 'org.apache.felix.framework', version= '4.2.1')
import org.apache.felix.framework.FrameworkFactory
import org.apache.felix.framework.Felix

props = new HashMap<String, Object>()
ff = new FrameworkFactory()
f = ff.newFramework(props)

f.init()
f.start()

context = f.getBundleContext()

context.installBundle("file://" + new File(".").getAbsolutePath() + "/pax-url-mvn-1.3.6.jar")

startall(context)

context.installBundle("mvn:org.apache.felix/org.apache.felix.shell/1.4.3")
context.installBundle("mvn:org.apache.felix/org.apache.felix.shell.tui/1.4.1")

startall(context)

/**
*
*/
def startall(context) {
  context.getBundles().each{ b ->
    try {
      b.start()
    } catch(Exception e) {
    }
  }
}