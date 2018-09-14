/**
 * (C) Copyright IBM Corporation 2018.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
	}
}