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

public class BoostRunTask extends AbstractBoostTask {

	BoostRunTask() {
        configure({
            description 'Runs the Boost application in the foreground.'
            logging.level = LogLevel.INFO
            group 'Boost'

            finalizedBy 'libertyRun'

            doFirst {
                logger.info('Running the application in the foreground.')
            }
        })
    }

	// @TaskAction
	// void run() {
		
	// }
}