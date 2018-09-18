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

import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.logging.LogLevel

import io.openliberty.boost.utils.GradleProjectUtil

import net.wasdev.wlp.gradle.plugins.extensions.PackageAndDumpExtension

public class BoostPackageTask extends AbstractBoostTask {

    String springVersion = GradleProjectUtil.findSpringBootVersion(this.project)

    BoostPackageTask() {
        configure({
            description 'Packages the application into an executable Liberty jar.'
            logging.level = LogLevel.INFO
            group 'Boost'

            finalizedBy 'libertyPackage'

            PackageAndDumpExtension boostPackage = new PackageAndDumpExtension()

            boostPackage.archive = "Boost.jar"
            boostPackage.include = "runnable"

            doFirst {
                project.liberty.server.packageLiberty = boostPackage

                logger.info('Packaging the applicaiton.')
            }
        })
    }   

    private boolean isSpringProject() {
        return springVersion != null && !springVersion.isEmpty()
    }

    private List<String> getSpringBootStarters() {

        List<String> springBootStarters = new ArrayList<String>();

        //Getting PublishArtifactSet then FileCollection then Set<File> from the project's configurationContainer
        Set<File> artifacts = configurations.archives.getFiles().getFiles()
        for (File art : artifacts) {
            if (art.getName().contains("spring-boot-starter")) {
                springBootStarters.add(art.getName());
            }
        }

        return springBootStarters;
    }
}