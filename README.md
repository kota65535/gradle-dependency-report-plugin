# gradle-dependency-report-plugin

Gradle plugin for dependency reporting in JSON format.

## Tasks

### `jsonDependencyReport`

Generates a dependency report in JSON format.
This task relies on [JsonProjectDependencyRenderer](https://github.com/gradle/gradle/blob/master/subprojects/diagnostics/src/main/java/org/gradle/api/reporting/dependencies/internal/JsonProjectDependencyRenderer.java), 
which is internally used by [HtmlDependencyReportTask](https://docs.gradle.org/current/dsl/org.gradle.api.reporting.dependencies.HtmlDependencyReportTask.html).

#### Properties

```
jsonDependencyReport {
  // The file which the report will be written to
  outputFile = file("${project.buildDir}/reports/project/dependencies.json")
}
```
