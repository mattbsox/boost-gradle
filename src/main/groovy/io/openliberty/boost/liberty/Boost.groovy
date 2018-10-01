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
package io.openliberty.boost.liberty

import org.gradle.api.*
import net.wasdev.wlp.gradle.plugins.extensions.ServerExtension

public class Boost implements Plugin<Project> {

    final String BOOST_SERVER_NAME = 'BoostServer'

    ServerExtension boostServer

    void apply(Project project) {
        project.extensions.create('boost', BoostExtension)

        project.pluginManager.apply('net.wasdev.wlp.gradle.plugins.Liberty')

        configureBoostServerProperties()

        project.liberty.server = boostServer

        new BoostTaskFactory(project).createTasks()
    }

    void configureBoostServerProperties() {
        boostServer = new ServerExtension()
        boostServer.name = BOOST_SERVER_NAME
        boostServer.looseApplication = false
    }
}