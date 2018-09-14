
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.IOException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

import static org.gradle.testkit.runner.TaskOutcome.*

public class BoostFunctionalTest extends AbstractBoostTest {

    String buildFileContent = "buildscript {\n\trepositories {\n\t\tmavenLocal()\n\t\tmavenCentral()\n\t}\n\tdependencies {\n\t\tclasspath 'io.openliberty.boost:boost-gradle-plugin:0.1-SNAPSHOT'\n\t}\n}\n\napply plugin: 'boost'"

    @Before
    void setup () {
        testProjectDir = new File(integTestDir, 'BoostFunctionalTest')
        
        createDir(testProjectDir)
        writeFile(new File(testProjectDir, 'build.gradle'), buildFileContent)
    }

    @Test
    public void testStartAndStopTasks() throws IOException {
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir)
            .withArguments("boostStart", "boostStop")
            .build()

        assertEquals(SUCCESS, result.task(":boostStart").getOutcome())
        assertEquals(SUCCESS, result.task(":boostStop").getOutcome())
    }
}
