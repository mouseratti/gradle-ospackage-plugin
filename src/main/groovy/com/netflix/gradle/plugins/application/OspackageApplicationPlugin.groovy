/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.gradle.plugins.application

import com.netflix.gradle.plugins.deb.Deb
import com.netflix.gradle.plugins.packaging.ProjectPackagingExtension
import com.netflix.gradle.plugins.packaging.SystemPackagingPlugin
import com.netflix.gradle.plugins.rpm.Rpm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.distribution.plugins.DistributionPlugin
import org.gradle.api.internal.IConventionAware
import org.gradle.api.plugins.ApplicationPlugin

/**
 * Combine the os-package with the Application plugin. Currently heavily opinionated to where
 * the code will live, though that is slightly configurable using the ospackage-application extension.
 *
 * TODO Make a base plugin, so that this plugin can require os-package
 *
 * Usage:
 * <ul>
 *     <li>User has to provide a mainClassName
 *     <li>User has to create SystemPackaging tasks, the easiest way is to apply plugin: 'os-package'
 * </ul>
 */
class OspackageApplicationPlugin implements Plugin<Project> {
    OspackageApplicationExtension extension

    @Override
    void apply(Project project) {
        extension = project.extensions.create('ospackage_application', OspackageApplicationExtension)
        ((IConventionAware) extension).conventionMapping.map('prefix') { '/opt'}

        project.plugins.apply(ApplicationPlugin)
        project.plugins.apply(SystemPackagingPlugin)

        // sets the location to $prefix/baseName-appendix-classifier
        project.tasks.distZip.version = ''

        final installDist = project.tasks.getByName(DistributionPlugin.TASK_INSTALL_NAME)

        final packagingExt = project.extensions.getByType(ProjectPackagingExtension)
        packagingExt.from {
            installDist.outputs.files.singleFile.parent
        }
        packagingExt.into { // Using a closure here to delay evaluation of prefix
            extension.getPrefix()
        }

        [Deb, Rpm].each { type ->
            project.tasks.withType(type) { task ->
                task.dependsOn(installDist)
            }
        }
    }
}
