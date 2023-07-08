# gradle-dependency-report-plugin

Gradle plugin for dependency reporting in JSON format.

## Requirements

- Gradle `>= 7.3`

## Tasks

### `jsonDependencyReport`

Generates a dependency report in JSON format. 
The JSON format refers to [JsonProjectDependencyRenderer](https://github.com/gradle/gradle/blob/master/subprojects/diagnostics/src/main/java/org/gradle/api/reporting/dependencies/internal/JsonProjectDependencyRenderer.java), 
which is internally used by [HtmlDependencyReportTask](https://docs.gradle.org/current/dsl/org.gradle.api.reporting.dependencies.HtmlDependencyReportTask.html).

#### Properties

This task generates a report for the task's containing project by default.
But it can also generate a report for multiple projects by setting the `projects` property.
```
jsonDependencyReport {
  projects = project.allprojects
}
```

The report file is generated as the `build/reports/project/dependencies.json` by default. 
This can also be changed by setting the `outputFile` property.
```
jsonDependencyReport {
  outputFile = file("${buildDir}/reports/project/dependencies.json")
}
```

