
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.IOException
import java.util.Collections

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import static org.gradle.testkit.runner.TaskOutcome.*

public class BoostFunctionalTest extends AbstractBoostTest {

    String buildFileContent = "buildscript {\n\trepositories {\n\t\tmavenLocal()\n\t\tmavenCentral()\n\t}\n\tdependencies {\n\t\tclasspath 'io.openliberty.boost:boost-gradle-plugin:0.1-SNAPSHOT'\n\t}\n}\n\napply plugin: 'boost'"
    //                          "plugins {" +
    //                              "id 'io.openliberty.boost'" +
    //                          "}\n"

    List<File> pluginClasspath

    @Before
    void setup () {
        testProjectDir = new File(integTestDir, 'BoostFunctionalTest')
        
        createDir(testProjectDir)
        writeFile(new File(testProjectDir, 'build.gradle'), buildFileContent)

        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
        pluginClasspath.each { println it.toString()}
    }

    @Test
    public void testStartAndStopTasks() throws IOException {
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("boostStart", "boostStop")
            //.withPluginClasspath(pluginClasspath)
            .build()

        assertEquals(SUCCESS, result.task(":boostStart").getOutcome())
        assertEquals(SUCCESS, result.task(":boostStop").getOutcome())
    }
}
