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

package io.openliberty.boost.liberty.tasks

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import org.gradle.api.logging.LogLevel

public class BoostStopTask extends AbstractBoostTask {

	BoostStopTask() {
        configure({
            description 'Stops the Boost application'
            logging.level = LogLevel.INFO
            group 'Boost'

            dependsOn 'libertyStop'

            doFirst {
                logger.info('Stopping the application.')
            }
        })
    }

	// @TaskAction
	// void stop() {
	// 	logger.info('Stopping the application.')
	// }
}