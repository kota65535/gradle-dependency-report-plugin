package io.github.kota65535.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ProjectReportsPluginConvention;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.plugins.internal.DefaultProjectReportsPluginConvention;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.util.internal.WrapUtil;

/**
 * <p>A {@link Plugin} which adds some project visualization report tasks to a project.</p>
 *
 * @see org.gradle.api.plugins.ProjectReportsPlugin
 * @see <a href="https://github.com/gradle/gradle/blob/master/subprojects/diagnostics/src/main/java/org/gradle/api/plugins/ProjectReportsPlugin.java">ProjectReportsPlugin.java</a>
 */
public class DependencyReportPlugin implements Plugin<Project> {
    
    static final String JSON_DEPENDENCY_REPORT = "jsonDependencyReport";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(ReportingBasePlugin.class);
        @SuppressWarnings("deprecation")
        final ProjectReportsPluginConvention convention = new DefaultProjectReportsPluginConvention(project);
        DeprecationLogger.whileDisabled(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                project.getConvention().getPlugins().put("projectReports", convention);
            }
        });

        project.getTasks().register(JSON_DEPENDENCY_REPORT, JsonDependencyReportTask.class, dependencyReportTask -> {
            dependencyReportTask.getProjectReportDirectory().convention(project.getLayout().dir(project.provider(() -> DeprecationLogger.whileDisabled(convention::getProjectReportDir))));
            dependencyReportTask.setDescription("Generates a report about your library dependencies in JSON format.");
            dependencyReportTask.conventionMapping("outputFile", () -> dependencyReportTask.getProjectReportDirectory().file("dependencies.json").get().getAsFile());
            dependencyReportTask.conventionMapping("projects", () -> WrapUtil.toSet(project));
        });
    }
}
