package io.github.kota65535.gradle.plugin;

import com.google.common.collect.Iterables;
import org.gradle.api.Project;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.api.tasks.diagnostics.internal.ConfigurationDetails;
import org.gradle.reporting.ReportRenderer;
import org.gradle.util.internal.GFileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JsonDependencyReporter extends ReportRenderer<Set<Project>, File> {

    private final JsonProjectDependencyRenderer renderer;

    public JsonDependencyReporter(VersionSelectorScheme versionSelectorScheme, VersionComparator versionComparator, VersionParser versionParser) {
        renderer = new JsonProjectDependencyRenderer(versionSelectorScheme, versionComparator, versionParser);
    }

    @Override
    public void render(Set<Project> projects, File outputFile) throws IOException {
        String json;

        if (projects.size() > 1) {
            Map<Project, Set<ConfigurationDetails>> projectsWithConfigurations = projects.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            this::getConfigurationDetails));
            json = renderer.render(projectsWithConfigurations);
        } else {
            Project project = Iterables.getOnlyElement(projects);
            json = renderer.render(project, getConfigurationDetails(project));
        }
        GFileUtils.writeFile(json, outputFile, "utf-8");
    }

    private Set<ConfigurationDetails> getConfigurationDetails(Project project) {
        return project.getConfigurations().stream()
                .map(ConfigurationDetails::of)
                .collect(Collectors.toSet());
    }
}
