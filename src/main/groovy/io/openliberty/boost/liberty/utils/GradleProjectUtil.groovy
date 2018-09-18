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
package io.openliberty.boost.utils

import java.util.Set

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

public class GradleProjectUtil {

    /**
     * Detect spring boot version dependency
     */
    public static String findSpringBootVersion(Project project) {
        String version = null;

        for ( Dependency dep : project.configurations.compile.getAllDependencies().toArray()) {
            if ("org.springframework.boot".equals(dep.getGroup()) && "spring-boot".equals(dep.getName())) {
                version = dep.getVersion()
                break
            }
        }

        return version        
    }

}