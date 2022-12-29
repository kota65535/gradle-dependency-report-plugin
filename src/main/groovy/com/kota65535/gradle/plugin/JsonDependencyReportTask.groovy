package com.kota65535.gradle.plugin


import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.api.tasks.diagnostics.ConventionReportTask
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer
import org.gradle.internal.logging.ConsoleRenderer

import javax.inject.Inject

@UntrackedTask(because = "We can't describe the dependency tree of all projects as input")
abstract class JsonDependencyReportTask extends ConventionReportTask {
    private Set<Project> projects
    private final DirectoryProperty reportDir

    JsonDependencyReportTask() {
        reportDir = getObjectFactory().directoryProperty()
    }
     
    @Internal
    DirectoryProperty getProjectReportDirectory() {
        return reportDir
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException()
    }

    @Inject
    protected VersionSelectorScheme getVersionSelectorScheme() {
        throw new UnsupportedOperationException()
    }

    @Inject
    protected VersionComparator getVersionComparator() {
        throw new UnsupportedOperationException()
    }

    @Inject
    protected  VersionParser getVersionParser() {
        throw new UnsupportedOperationException()
    }

    @Override
    protected ReportRenderer getRenderer() {
        throw new UnsupportedOperationException()
    }

    @TaskAction
    void generate() {
        JsonDependencyReporter reporter = new JsonDependencyReporter(getVersionSelectorScheme(), getVersionComparator(), getVersionParser())
        reporter.render(getProjects(), outputFile)

        getProject().getLogger().lifecycle("See the report at: {}", new ConsoleRenderer().asClickableFileUrl(outputFile))
    }

    @Internal
    Set<Project> getProjects() {
        return projects
    }

    void setProjects(Set<Project> projects) {
        this.projects = projects
    }
}
