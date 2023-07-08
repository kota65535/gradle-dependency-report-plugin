package io.github.kota65535.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.diagnostics.ConventionReportTask;
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.internal.serialization.Transient;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

import static java.util.Collections.singleton;
import static org.gradle.internal.Cast.uncheckedCast;

@UntrackedTask(because = "We can't describe the dependency tree of all projects as input")
public abstract class JsonDependencyReportTask extends ConventionReportTask {
    private final Transient.Var<Set<Project>> projects = Transient.varOf(uncheckedCast(singleton(getProject())));
    private final DirectoryProperty reportDir;

    public JsonDependencyReportTask() {
        reportDir = getObjectFactory().directoryProperty();
    }

    @Override
    protected ReportRenderer getRenderer() {
        throw new UnsupportedOperationException();
    }

    @Internal
    public DirectoryProperty getProjectReportDirectory() {
        return reportDir;
    }

    @Inject
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected VersionSelectorScheme getVersionSelectorScheme() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected VersionComparator getVersionComparator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected  VersionParser getVersionParser() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void generate() throws IOException {
        JsonDependencyReporter reporter = new JsonDependencyReporter(getVersionSelectorScheme(), getVersionComparator(), getVersionParser());
        reporter.render(projects.get(), getOutputFile());
        getLogger().lifecycle("See the report at: {}", new ConsoleRenderer().asClickableFileUrl(getOutputFile()));
    }

    @Internal
    public Set<Project> getProjects() {
        return projects.get();
    }

    public void setProjects(Set<Project> projects) {
        this.projects.set(projects);
    }
}
