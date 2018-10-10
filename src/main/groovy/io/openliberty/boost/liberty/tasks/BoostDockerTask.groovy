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

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel
import org.gradle.util.ConfigureUtil

import io.openliberty.boost.utils.GradleProjectUtil

import net.wasdev.wlp.common.plugins.util.SpringBootUtil

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.charset.Charset
import java.util.ArrayList

public class BoostDockerTask extends AbstractBoostTask {

    protected static final String LIBERTY_IMAGE_1 = "open-liberty:springBoot1"
    protected static final String LIBERTY_IMAGE_2 = "open-liberty:springBoot2"
    protected static final String LIB_INDEX_CACHE = "lib.index.cache"
    protected static final String ARG_SOURCE_APP = "--sourceAppPath"
    protected static final String ARG_DEST_THIN_APP = "--targetThinAppPath"
    protected static final String ARG_DEST_LIB_CACHE = "--targetLibCachePath"
    protected static final String FROM = "FROM "
    protected static final String COPY = "COPY "
    protected static final String RUN = "RUN "

    String springBootVersion
    File appFile

    BoostDockerTask() {
        configure({
            description 'Dockerizes a Boost project.'
            logging.level = LogLevel.INFO
            group 'Boost'

            finalizedBy 'docker'

            project.afterEvaluate {
                springBootVersion = GradleProjectUtil.findSpringBootVersion(project)
                if (springBootVersion != null) {
                    if (springBootVersion.startsWith("2.")) {
                        dependsOn 'bootJar'
                        appFile = project.bootJar.archivePath
                    } else if (springBootVersion.startsWith("1.")){
                        dependsOn 'bootRepackage'
                        if (project.plugins.hasPlugin('java')) {
                            appFile = project.jar.archivePath
                            //Checking for classifier in bootRepackage and adding to archiveName
                            if (project.bootRepackage.classifier != null && !project.bootRepackage.classifier.isEmpty()) {
                                String appArchiveName = //Adding classifier to the appArchive name
                                    appFile.getName().substring(0, appFile.getName().lastIndexOf(".")) +
                                    '-' + 
                                    project.bootRepackage.classifier.toString() + 
                                    appFile.getName().substring(appFile.getName().lastIndexOf("."))
                                appFile = new File(appFile.getParent(), appArchiveName)
                            }
                        }
                    } //JEE case here
                    configureDockerPlugin()  
                }
                project.dockerClean.enabled = false
                project.dockerPrepare.enabled = false
            }
            
            doFirst {
                if (appFile == null) { //if we didn't set the appName during configuration we can get it from the project
                    if (!project.configurations.archives.allArtifacts.isEmpty()) {
                        appFile = project.configurations.archives.allArtifacts[0].getFile()
                        project.docker.setName(appFile.getName())
                    } else {
                        throw new GradleException ('Unable to determine the project artifact name.')
                    }
                }
                createDockerFile()
            }
        })
    }

    public void createDockerFile() throws GradleException {
        if (springBootVersion != null) {
            createSpringBootDockerFile(springBootVersion)
            //Need to add the application to the build/docker directory for the docker plugin to work
            copyAppToDockerDir()
        } else {
            throw new GradleException("Unable to create a Docker image because application type is not supported.")
        }
    }

    //Could get the archiveName from the configurations.archives
    protected void configureDockerPlugin() {
        if (project.boost.dockerRepo != null && !project.boost.dockerRepo.isEmpty()) {
            project.docker.setName(project.boost.dockerRepo + '/' + appFile.getName())
        } else {
            project.docker.setName(appFile.getName())
        }

        project.docker.setDockerfile(new File(project.projectDir, 'Dockerfile'))
        project.docker.buildArgs(['APP_FILE': appFile.getName()])
        project.docker.tags('latest')
    }

    //Copies application artifact and Dockerfile to build/docker
    protected void copyAppToDockerDir() {
        File dockerDir = new File(project.buildDir, "docker")
        Files.copy(appFile.toPath(), new File(dockerDir, appFile.getName()).toPath())
        Files.copy(new File(project.projectDir, "Dockerfile").toPath(), new File(dockerDir, "Dockerfile").toPath())
    }

    protected void createSpringBootDockerFile(String springBootVersion) throws GradleException, IOException {
        try {
            if (SpringBootUtil.isSpringBootUberJar(appFile)) {
                File dockerDir = new File(project.buildDir, 'docker')
                Files.createDirectory(dockerDir.toPath())
                File dockerFile = createNewDockerFile(project.projectDir)
                String libertySBImage = getLibertySpringBootBaseImage(springBootVersion)
                writeSpringBootDockerFile(dockerFile, libertySBImage)
            } else {
                throw new GradleException(appFile.getAbsolutePath() + " file is not an executable archive. "
                        + "Please rebuild the archive with the bootJar or bootRepackage tasks.")
            }
        } catch (FileAlreadyExistsException e1) {
            logger.warn("Dockerfile already exists.")
        }
    }

    protected File createNewDockerFile(File dockerFileDirectory) {
        File dockerFile = new File(dockerFileDirectory, "Dockerfile")
        Files.createFile(dockerFile.toPath())
        logger.info("Creating Dockerfile: " + dockerFile.getAbsolutePath())
        return dockerFile
    }

    protected String getLibertySpringBootBaseImage(String springBootVersion) throws GradleException {
        String libertyImage = null

        if (springBootVersion.startsWith("1.")) {
            libertyImage = LIBERTY_IMAGE_1
        } else if (springBootVersion.startsWith("2.")) {
            libertyImage = LIBERTY_IMAGE_2
        } else {
            throw new GradleException(
                    "No supporting docker image found for Open Liberty for the Spring Boot version "
                            + springBootVersion)
        }
        return libertyImage
    }

    private void writeSpringBootDockerFile(File dockerFile, String libertyImage) throws IOException {
        ArrayList<String> lines = new ArrayList()
        lines.add(FROM + libertyImage + " as " + "staging")

        lines.add("\n")
        lines.add("# The APP_FILE ARG provides the final name of the Spring Boot application archive")
        lines.add("ARG" + " " + "APP_FILE")

        lines.add("\n")
        lines.add("# Stage the fat JAR")
        lines.add(COPY + "\${APP_FILE}" + " " + "/staging/" + "\${APP_FILE}")

        lines.add("\n")
        lines.add("# Thin the fat application; stage the thin app output and the library cache")
        lines.add(RUN + "springBootUtility thin " + ARG_SOURCE_APP + "=" + "/staging/" + "\${APP_FILE}" + " "
                + ARG_DEST_THIN_APP + "=" + "/staging/" + "thin-\${APP_FILE}" + " " + ARG_DEST_LIB_CACHE + "="
                + "/staging/" + LIB_INDEX_CACHE)

        lines.add("\n")
        lines.add("# Final stage, only copying the liberty installation (includes primed caches)")
        lines.add("# and the lib.index.cache and thin application")
        lines.add(FROM + libertyImage)

        lines.add("\n")
        lines.add("ARG" + " " + "APP_FILE")

        lines.add("\n")
        lines.add(COPY + "--from=staging " + "/staging/" + LIB_INDEX_CACHE + " " + "/" + LIB_INDEX_CACHE)

        lines.add("\n")
        lines.add(COPY + "--from=staging " + "/staging/thin-\${APP_FILE}" + " "
                + "/config/dropins/spring/thin-\${APP_FILE}")

        Files.write(dockerFile.toPath(), lines, Charset.forName("UTF-8"))
    }

}