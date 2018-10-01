/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.boost.liberty.tasks

import java.util.ArrayList
import java.io.OutputStream
import java.io.FileOutputStream
import java.io.IOException

import org.apache.commons.io.FileUtils

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.TransformerException

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel
import org.gradle.api.artifacts.Dependency
import org.gradle.tooling.BuildException

import io.openliberty.boost.utils.GradleProjectUtil
import io.openliberty.boost.utils.BoostUtil
import io.openliberty.boost.utils.LibertyServerConfigGenerator
import io.openliberty.boost.utils.SpringBootUtil
import io.openliberty.boost.BoostLoggerI

import net.wasdev.wlp.gradle.plugins.extensions.PackageAndDumpExtension

public class BoostPackageTask extends AbstractBoostTask {

    String springBootVersion = GradleProjectUtil.findSpringBootVersion(this.project)

    BoostPackageTask() {
        configure({
            description 'Packages the application into an executable Liberty jar.'
            logging.level = LogLevel.INFO
            group 'Boost'

            dependsOn 'libertyCreate'

            //There are some things that this task does before we can package up the server into a JAR
            PackageAndDumpExtension boostPackage = new PackageAndDumpExtension()

            //We have to check these properties after the build.gradle file has been evaluated.
            //Some properties could get set after our plugin is applied.
            project.afterEvaluate {
                if (isSpringProject()) {
                    if(springBootVersion.startsWith('2.0')) {
                        boostPackage.archive = project.bootJar.archivePath.toString()
                        dependsOn 'bootJar'
                    } // else { //Assume 1.5?
                    //     //No bootJar task so we have to get the archive name from the jar or war tasks
                    //     if (project.plugins.hasPlugin('java')) {
                    //         //Going to use the first task archiveName for the boostJar name
                    //         boostPackage.archive = project.jar.archiveName 
                    //     } else if (project.plugins.hasPlugin('war')) { //Might also need a case for wars
                    //         boostPackage.archive = project.war.archiveName
                    //     } else {
                    //         boostPackage.archive = 'BoostApp.jar'
                    //     }

                    //     dependsOn 'bootRepackage'
                    // }

                    //Doing this until installSpringBootApp is added to installApps
                    project.tasks.getByName('installApps').enabled = false

                    //installFeature should check the server.xml in the server directory and install the missing features
                    project.tasks.getByName('libertyPackage').dependsOn 'installSpringBootApp', 'installFeature'
                    finalizedBy 'libertyPackage'
                } else { //JavaEE projects

                    //Need to check features and generate server.xml

                    dependsOn 'installApps'

                    finalizedBy 'libertyPackage'

                    //WAR and EAR projects will be packaged as zips even with include set to 'minify, runnable'
                    if(project.plugins.hasPlugin('war')) {
                        boostPackage.archive = project.tasks.getByName('war').archiveName
                    } else if (project.plugins.hasPlugin('ear')) {
                        boostPackage.archive = project.tasks.getByName('ear').archiveName
                    } else {
                        boostPackage.archive = 'BoostApp.jar'
                    }
                }
                boostPackage.include = "runnable, minify"
            }

            //The task will perform this before any other task actions
            doFirst {
                // boostPackage.archive = "${project.buildDir}/" + boostPackage.archive
                project.liberty.server.packageLiberty = boostPackage

                if (isSpringProject()) {
                    if(springBootVersion.startsWith('2.0')) {
                        File springUberJar = project.bootJar.archivePath
                        validateSpringBootUberJAR(springUberJar)
                        copySpringBootUberJar(springUberJar)
                    }
                }

                createServerXml()
                generateBootstrapProps()

                logger.info('Packaging the applicaiton.')
            }
        })
    }   

    public boolean isSpringProject() {
        return springBootVersion != null && !springBootVersion.isEmpty()
    }

    protected List<String> getSpringBootStarters() {

        List<String> springBootStarters = new ArrayList<String>()

        project.configurations.compile.dependencies.each { Dependency art ->
            if (art.getName().contains("spring-boot-starter")) {
                springBootStarters.add(art.getName())
            }
        }

        return springBootStarters
    }

    public String getClassifier() {
        String classifier = null

        if (isSpringProject()) {
            if (springBootVersion.startsWith('2.0')) {
                classifierproject.tasks.getByName('bootJar').classifier
            }
        }

        return classifier
    }

    protected void createServerXml() throws TransformerException, ParserConfigurationException {
        if (isSpringProject()) {
            LibertyServerConfigGenerator serverConfig = new LibertyServerConfigGenerator()

            // Find and add appropriate springBoot features
            List<String> featuresNeededForSpringBootApp = SpringBootUtil.getLibertyFeaturesForSpringBoot(springBootVersion,
                    getSpringBootStarters(), new BoostLogger())
            serverConfig.addFeatures(featuresNeededForSpringBootApp)

            serverConfig.addHttpEndpoint(null, "\${server.port}", null)

            // Write server.xml to Liberty server config directory
            serverConfig.writeToServer("${project.buildDir}/wlp/usr/servers/" + project.liberty.server.name)
        }
    }

    protected void generateBootstrapProps() throws GradleException {

        Properties springBootProps = new Properties()

        try {
            springBootProps = SpringBootUtil.getSpringBootServerProperties(project.buildDir.toString())

        } catch (IOException e) {
            throw new GradleException("Unable to read properties from Spring Boot application", e)
        }

        // Write properties to bootstrap.properties
        OutputStream output = null

        try {
            output = new FileOutputStream(
                    "${project.buildDir}/wlp/usr/servers/" + project.liberty.server.name + "/bootstrap.properties")
            springBootProps.store(output, null)

        } catch (IOException io) {
            throw new GradleException("Unable to write properties to bootstrap.properties file", io)
        } finally {
            if (output != null) {
                try {
                    output.close()
                } catch (IOException io_finally) {
                    throw new GradleException("Unable to write properties to bootstrap.properties file",
                            io_finally)
                }
            }
        }
    }

    protected void copySpringBootUberJar(File springBootUberJar) throws GradleException {
        try {
            File springBootUberJarCopy = null
            if (springBootUberJar != null) { // Only copy the Uber JAR if it is valid
                springBootUberJarCopy = SpringBootUtil.copySpringBootUberJar(springBootUberJar,
                        new BoostLogger())
            }

            if (springBootUberJarCopy == null) {
                logger.info('Plugin should replace the project archive: ' + shouldReplaceProjectArchive())
                if (shouldReplaceProjectArchive()) {
                    if (!project.configurations.archives.allArtifacts.isEmpty()) {  
                        File springJar = new File(
                            SpringBootUtil.getBoostedSpringBootUberJarPath(project.configurations.archives.allArtifacts[0].getFile()))
                        if (net.wasdev.wlp.common.plugins.util.SpringBootUtil.isSpringBootUberJar(springJar)) {
                            logger.info("Copying back Spring Boot Uber JAR as project artifact.")
                            FileUtils.copyFile(springJar, project.configurations.archives.allArtifacts[0].getFile())
                        }
                    }
                }
            } else {
                logger.info("Copied Spring Boot Uber JAR to " + springBootUberJarCopy.getCanonicalPath())
            }
        } catch (BuildException | IOException e) {
            throw new GradleException(e.getMessage(), e)
        }
        
    }

    protected void validateSpringBootUberJAR(File springBootUberJar) throws GradleException {
        if (!project.configurations.archives.allArtifacts.isEmpty()) {
            if (!BoostUtil.isLibertyJar(project.configurations.archives.allArtifacts[0].getFile(), new BoostLogger())
                && springBootUberJar == null) {
            throw new GradleException (
                    "A valid Spring Boot Uber JAR was not found. Run the 'bootJar' task and try again.")
            }
        }
    }

    //returns true if bootJar is using the same archiveName as jar
    private boolean shouldReplaceProjectArchive() {
        if (project.plugins.hasPlugin('java')) {
            return project.jar.archiveName == project.bootJar.archiveName
        }
        return false
    }

    private class BoostLogger implements BoostLoggerI {

        @Override
        public void debug(String msg) {
            logger.debug(msg)
        }

        @Override
        public void debug(String msg, Throwable e) {
            logger.debug(msg, e)
        }

        @Override
        public void debug(Throwable e) {
            logger.debug(e)
        }

        @Override
        public void warn(String msg) {
            logger.warn(msg)
        }

        @Override
        public void info(String msg) {
            logger.info(msg)
        }

        @Override
        public void error(String msg) {
            logger.error(msg)
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isEnabled(LogLevel.DEBUG)
        }
    }
}