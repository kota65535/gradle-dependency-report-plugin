package com.kota65535.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.ProjectReportsPluginConvention
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.plugins.internal.DefaultProjectReportsPluginConvention
import org.gradle.util.internal.WrapUtil

class DependencyReportPlugin implements Plugin<Project> {
    
    static final String JSON_DEPENDENCY_REPORT = "jsonDependencyReport"
    
    void apply(Project project) {
        // cf. https://github.com/gradle/gradle/blob/master/subprojects/diagnostics/src/main/java/org/gradle/api/plugins/ProjectReportsPlugin.java
        project.getPluginManager().apply(ReportingBasePlugin.class)
        final ProjectReportsPluginConvention convention = new DefaultProjectReportsPluginConvention(project)
        Convention projectConvention = project.getConvention()
        projectConvention.getPlugins().put("projectReports", convention)

        project.getTasks().register(JSON_DEPENDENCY_REPORT, JsonDependencyReportTask.class, dependencyReportTask -> {
            dependencyReportTask.getProjectReportDirectory().convention(project.getLayout().dir(project.provider(() -> convention.getProjectReportDir())))
            dependencyReportTask.setDescription("Generates a report about your library dependencies in JSON format.")
            dependencyReportTask.conventionMapping("outputFile", () -> dependencyReportTask.getProjectReportDirectory().file("dependencies.json").get().getAsFile())
            dependencyReportTask.conventionMapping("projects", () -> WrapUtil.toSet(project))
        })
    }
}
