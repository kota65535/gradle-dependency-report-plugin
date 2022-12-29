/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kota65535.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme
import org.gradle.api.reporting.dependencies.internal.JsonProjectDependencyRenderer
import org.gradle.util.internal.GFileUtils

class JsonDependencyReporter {
    private final JsonProjectDependencyRenderer renderer

    JsonDependencyReporter(VersionSelectorScheme versionSelectorScheme, VersionComparator versionComparator, VersionParser versionParser) {
        renderer = new JsonProjectDependencyRenderer(versionSelectorScheme, versionComparator, versionParser)
    }

    void render(final Set<Project> projects, File outputFile) {
        projects.forEach(p -> {
            String json = renderer.render(p)
            GFileUtils.writeFile(json, outputFile, "utf-8")
        })
    }
}
