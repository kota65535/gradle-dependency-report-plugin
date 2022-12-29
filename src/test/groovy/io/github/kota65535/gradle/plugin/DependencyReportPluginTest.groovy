package io.github.kota65535.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class DependencyReportPluginTest extends Specification {
    @TempDir
    File testProjectDir
    File buildFile

    def setup() {
        buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'io.github.kota65535.dependency-report'
            }
        """
    }

    def "create dependency report json file"() {
        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(DependencyReportPlugin.JSON_DEPENDENCY_REPORT)
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("See the report at:")
        assert new File("${testProjectDir.absolutePath}/build/reports/project/dependencies.json").exists()
        result.task(":${DependencyReportPlugin.JSON_DEPENDENCY_REPORT}").outcome == TaskOutcome.SUCCESS
    }
    
    def "create dependency report json file with outputFile option"() {
        given:
        buildFile << """
            jsonDependencyReport {
                outputFile = file("build/a.json")
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('jsonDependencyReport')
                .withPluginClasspath()
                .build()

        then:
        assert new File("${testProjectDir.absolutePath}/build/a.json").exists()
        result.output.contains("See the report at:")
        result.task(":${DependencyReportPlugin.JSON_DEPENDENCY_REPORT}").outcome == TaskOutcome.SUCCESS
    }
}
