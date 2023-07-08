package io.github.kota65535.gradle.plugin;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import groovy.json.JsonBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionComparator;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionSelectorScheme;
import org.gradle.api.reporting.dependencies.internal.StrictDependencyResultSpec;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency;
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult;
import org.gradle.api.tasks.diagnostics.internal.insight.DependencyInsightReporter;
import org.gradle.internal.deprecation.DeprecatableConfiguration;
import org.gradle.util.GradleVersion;
import org.gradle.util.internal.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.kota65535.gradle.plugin.InternalChangeHandler.*;

/**
 * Renderer that emits a JSON tree containing the HTML dependency report structure for a given project. The structure is the following:
 *
 * <pre>
 *     {
 *          "gradleVersion" : "...",
 *          "generationDate" : "...",
 *          "project" : {
 *               "name" : "...",
 *               "description : "...", (optional)
 *               "configurations" : [
 *                   "name" : "...",
 *                   "description" : "...", (optional)
 *                   "dependencies" : [
 *                       {
 *                           "module" : "group:name"
 *                           "name" : "...",
 *                           "resolvable" : true|false,
 *                           "alreadyRendered" : true|false
 *                           "hasConflict" : true|false
 *                           "children" : [
 *                               same array as configurations.dependencies.children
 *                           ]
 *                       },
 *                       ...
 *                   ],
 *                   "moduleInsights : [
 *                       {
 *                           "module" : "group:name"
 *                           "insight" : [
 *                               {
 *                                   "name" : "...",
 *                                   "description" : "...",
 *                                   "resolvable" : true|false,
 *                                   "hasConflict" : true|false,
 *                                   "children": [
 *                                       {
 *                                           "name" : "...",
 *                                           "resolvable" : "...",
 *                                           "hasConflict" : true|false,
 *                                           "alreadyRendered" : true|false
 *                                           "isLeaf" : true|false
 *                                           "children" : [
 *                                               same array as configurations.moduleInsights.insight.children
 *                                           ]
 *                                       },
 *                                       ...
 *                                   ]
 *                               },
 *                               ...
 *                           ]
 *                       }
 *                       ,
 *                       ...
 *                   ]
 *               ]
 *          }
 *      }
 * </pre>
 */
class JsonProjectDependencyRenderer {
    public JsonProjectDependencyRenderer(VersionSelectorScheme versionSelectorScheme, VersionComparator versionComparator, VersionParser versionParser) {
        this.versionSelectorScheme = versionSelectorScheme;
        this.versionComparator = versionComparator;
        this.versionParser = versionParser;
    }

    /**
     * Generates the project dependency report structure
     *
     * @param project the project for which the report must be generated
     * @return the generated JSON, as a String
     */
    public String render(Project project) {
        JsonBuilder json = new JsonBuilder();
        renderProject(project, json);
        return json.toString();
    }

    public String render(Set<Project> projects) {
        JsonBuilder json = new JsonBuilder();
        renderProjects(projects, json);
        return json.toString();
    }

    // Historic note: this class still uses the Groovy JsonBuilder, as it was originally developed as a Groovy class.
    private void renderProject(Project project, JsonBuilder json) {

        Map<String, Object> overall = Maps.newLinkedHashMap();
        overall.put("gradleVersion", GradleVersion.current().toString());
        overall.put("generationDate", new Date().toString());

        Map<String, Object> projectOut = Maps.newLinkedHashMap();
        projectOut.put("name", project.getName());
        projectOut.put("description", project.getDescription());
        projectOut.put("configurations", createConfigurations(project));
        overall.put("project", projectOut);

        json.call(overall);
    }

    private void renderProjects(Set<Project> projects, JsonBuilder json) {

        Map<String, Object> overall = Maps.newLinkedHashMap();
        overall.put("gradleVersion", GradleVersion.current().toString());
        overall.put("generationDate", new Date().toString());

        List<Map<String, Object>> projectsOut = projects.stream()
                .map(p -> {
                    Map<String, Object> projectOut = Maps.newLinkedHashMap();
                    projectOut.put("name", p.getName());
                    projectOut.put("description", p.getDescription());
                    projectOut.put("configurations", createConfigurations(p));
                    return projectOut;
                })
                .collect(Collectors.toList());
        overall.put("projects", projectsOut);

        json.call(overall);
    }

    private List<Configuration> getConfigurationsWhichCouldHaveDependencyInfo(Project project) {
        List<Configuration> filteredConfigurations = new ArrayList<>();
        for (Configuration configuration : project.getConfigurations()) {
            ConfigurationInternal configurationInternal = (ConfigurationInternal)configuration;
            if (configurationInternalIsDeclarableByExtension(configurationInternal)) {
                filteredConfigurations.add(configuration);
            }
        }
        return filteredConfigurations;
    }

    private boolean canBeResolved(Configuration configuration) {
        // cf. https://github.com/gradle/gradle/blob/5f4a070a62a31a17438ac998c2b849f4f6892877/subprojects/diagnostics/src/main/java/org/gradle/api/tasks/diagnostics/internal/ConfigurationDetails.java#L41
        boolean isDeprecatedForResolving = DeprecatableConfigurationIsDeprecatedForResolution((DeprecatableConfiguration)configuration);
        return configuration.isCanBeResolved() && !isDeprecatedForResolving;
    }

    private List<Map<String, Object>> createConfigurations(Project project) {
        Iterable<Configuration> configurations = getConfigurationsWhichCouldHaveDependencyInfo(project);
        return CollectionUtils.collect(configurations, configuration -> {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>(4);
            map.put("name", configuration.getName());
            map.put("description", configuration.getDescription());
            map.put("dependencies", createDependencies(configuration));
            map.put("moduleInsights", createModuleInsights(configuration));
            return map;
        });
    }

    private List<Map<String, Object>> createDependencies(Configuration configuration) {
        if (canBeResolved(configuration)) {
            ResolutionResult result = configuration.getIncoming().getResolutionResult();
            RenderableDependency root = new RenderableModuleResult(result.getRoot());
            return createDependencyChildren(root, new HashSet<>());
        } else {
            return createDependencyChildren(createUnresolvableConfigurationResult(configuration), new HashSet<>());
        }
    }

    private List<Map<String, Object>> createDependencyChildren(RenderableDependency dependency, final Set<Object> visited) {
        Iterable<? extends RenderableDependency> children = dependency.getChildren();
        return CollectionUtils.collect(children, childDependency -> {
            boolean alreadyVisited = !visited.add(childDependency.getId());
            boolean alreadyRendered = alreadyVisited && !childDependency.getChildren().isEmpty();
            String name = replaceArrow(childDependency.getName());
            boolean hasConflict = !name.equals(childDependency.getName());
            LinkedHashMap<String, Object> map = new LinkedHashMap<>(6);
            ModuleIdentifier moduleIdentifier = getModuleIdentifier(childDependency);
            map.put("module", moduleIdentifier == null ? null : moduleIdentifier.toString());
            map.put("name", name);
            map.put("resolvable", childDependency.getResolutionState());
            map.put("hasConflict", hasConflict);
            map.put("alreadyRendered", alreadyRendered);
            map.put("children", Collections.emptyList());
            if (!alreadyRendered) {
                map.put("children", createDependencyChildren(childDependency, visited));
            }
            return map;
        });
    }

    @Nullable
    private ModuleIdentifier getModuleIdentifier(RenderableDependency renderableDependency) {
        if (renderableDependency.getId() instanceof ModuleComponentIdentifier) {
            ModuleComponentIdentifier id = (ModuleComponentIdentifier) renderableDependency.getId();
            return id.getModuleIdentifier();
        }
        return null;
    }

    private List<Object> createModuleInsights(final Configuration configuration) {
        Iterable<ModuleIdentifier> modules = collectModules(configuration);
        return CollectionUtils.collect(modules, moduleIdentifier -> createModuleInsight(moduleIdentifier, configuration));
    }

    private Set<ModuleIdentifier> collectModules(Configuration configuration) {
        RenderableDependency root;
        if (canBeResolved(configuration)) {
            ResolutionResult result = configuration.getIncoming().getResolutionResult();
            root = new RenderableModuleResult(result.getRoot());
        } else {
            root = createUnresolvableConfigurationResult(configuration);
        }
        Set<ModuleIdentifier> modules = Sets.newHashSet();
        Set<ComponentIdentifier> visited = Sets.newHashSet();
        populateModulesWithChildDependencies(root, visited, modules);
        return modules;
    }

    private void populateModulesWithChildDependencies(RenderableDependency dependency, Set<ComponentIdentifier> visited, Set<ModuleIdentifier> modules) {
        for (RenderableDependency childDependency : dependency.getChildren()) {
            ModuleIdentifier moduleId = getModuleIdentifier(childDependency);
            if (moduleId == null) {
                continue;
            }
            modules.add(moduleId);
            boolean alreadyVisited = !visited.add((ComponentIdentifier) childDependency.getId());
            if (!alreadyVisited) {
                populateModulesWithChildDependencies(childDependency, visited, modules);
            }
        }
    }

    private Map<String, Object> createModuleInsight(ModuleIdentifier module, Configuration configuration) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>(2);
        map.put("module", module.toString());
        map.put("insight", createInsight(module, configuration));
        return map;
    }

    private List<Object> createInsight(ModuleIdentifier module, final Configuration configuration) {
        final Spec<DependencyResult> dependencySpec = new StrictDependencyResultSpec(module);

        ResolutionResult result = configuration.getIncoming().getResolutionResult();
        final Set<DependencyResult> selectedDependencies = new LinkedHashSet<>();

        result.allDependencies(it -> {
            if (dependencySpec.isSatisfiedBy(it)) {
                selectedDependencies.add(it);
            }
        });

        Collection<RenderableDependency> sortedDeps = new DependencyInsightReporter(versionSelectorScheme, versionComparator, versionParser).convertToRenderableItems(selectedDependencies, false);
        return CollectionUtils.collect(sortedDeps, dependency -> {
            String name = replaceArrow(dependency.getName());
            LinkedHashMap<String, Object> map = new LinkedHashMap<>(5);
            map.put("name", replaceArrow(dependency.getName()));
            map.put("description", dependency.getDescription());
            map.put("resolvable", dependency.getResolutionState());
            map.put("hasConflict", !name.equals(dependency.getName()));
            map.put("children", createInsightDependencyChildren(dependency, new HashSet<>(), configuration));
            return map;
        });
    }

    private List<Object> createInsightDependencyChildren(RenderableDependency dependency, final Set<Object> visited, final Configuration configuration) {
        Iterable<? extends RenderableDependency> children = dependency.getChildren();
        return CollectionUtils.collect(children, childDependency -> {
            boolean alreadyVisited = !visited.add(childDependency.getId());
            boolean leaf = childDependency.getChildren().isEmpty();
            boolean alreadyRendered = alreadyVisited && !leaf;
            String childName = replaceArrow(childDependency.getName());
            boolean hasConflict = !childName.equals(childDependency.getName());
            String name = leaf ? configuration.getName() : childName;

            LinkedHashMap<String, Object> map = new LinkedHashMap<>(6);
            map.put("name", name);
            map.put("resolvable", childDependency.getResolutionState());
            map.put("hasConflict", hasConflict);
            map.put("alreadyRendered", alreadyRendered);
            map.put("isLeaf", leaf);
            map.put("children", Collections.emptyList());
            if (!alreadyRendered) {
                map.put("children", createInsightDependencyChildren(childDependency, visited, configuration));
            }
            return map;
        });
    }

    private String replaceArrow(String name) {
        return name.replace(" -> ", " \u27A1 ");
    }

    private final VersionSelectorScheme versionSelectorScheme;
    private final VersionComparator versionComparator;
    private final VersionParser versionParser;
}
