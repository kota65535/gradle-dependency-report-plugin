package io.github.kota65535.gradle.plugin

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
import spock.lang.TempDir

class DependencyReportPluginTest extends Specification {
    @TempDir
    File testProjectDir
    File settingsFile
    File buildFile
    File buildFileSub

    def setup() {
        settingsFile = new File(testProjectDir, 'settings.gradle')
        settingsFile << """
        rootProject.name = "foo"
        include "sub"
        """
        buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'io.github.kota65535.dependency-report'
            }
        """
        File subDir = new File(testProjectDir, "sub")
        subDir.mkdir()
        buildFileSub = new File(subDir, 'build.gradle')
        buildFileSub << """
            plugins {
              id "java"
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
        def outputFile = new File("${testProjectDir.absolutePath}/build/reports/project/dependencies.json")
        assert outputFile.exists()
        def outputJson = new JsonSlurper().parse(outputFile)
        assert outputJson.project != null
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
        def outputFile = new File("${testProjectDir.absolutePath}/build/a.json")
        assert outputFile.exists()
        result.output.contains("See the report at:")
        result.task(":${DependencyReportPlugin.JSON_DEPENDENCY_REPORT}").outcome == TaskOutcome.SUCCESS
    }

    def "create dependency report for all projects"() {
        given:
        buildFile << """
            jsonDependencyReport {
              projects = allprojects
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('jsonDependencyReport')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("See the report at:")
        def outputFile = new File("${testProjectDir.absolutePath}/build/reports/project/dependencies.json")
        assert outputFile.exists()
        def outputJson = new JsonSlurper().parse(outputFile)
        assert outputJson.projects.size() == 2
        result.task(":${DependencyReportPlugin.JSON_DEPENDENCY_REPORT}").outcome == TaskOutcome.SUCCESS
    }
}
