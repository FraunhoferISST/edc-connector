/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */


plugins {
    `java-library`
    pmd
    id("com.github.spotbugs") version "5.2.1"
}

val javaVersion: String by project
val edcScmUrl: String by project
val edcScmConnection: String by project

buildscript {
    dependencies {
        val version: String by project
        classpath("org.eclipse.edc.edc-build:org.eclipse.edc.edc-build.gradle.plugin:$version")
    }
}

allprojects {
    apply(plugin = "${group}.edc-build")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")

    configure<org.eclipse.edc.plugins.edcbuild.extensions.BuildExtension> {
        pom {
            scmUrl.set(edcScmUrl)
            scmConnection.set(edcScmConnection)
        }
    }

    configure<CheckstyleExtension> {
        configFile = rootProject.file("resources/edc-checkstyle-config.xml")
        configDirectory.set(rootProject.file("resources"))
    }
    // Configure PMD
    configure<PmdExtension> {
        isConsoleOutput = true
        toolVersion = "6.55.0" // Use a recent version
        ruleSets = listOf() // Empty ruleSets is required to use a custom ruleset file
        isIgnoreFailures = true
        ruleSetFiles = files("${rootProject.projectDir}/resources/edc-pmd-ruleset.xml")
    }

    // SpotBugs configuration
    extensions.configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion.set("4.7.3")
        ignoreFailures.set(true)
        showProgress.set(true)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)

        // Optional filters
        includeFilter.set(file("${rootProject.projectDir}/resources/edc-spotbugs-include.xml"))
        // excludeFilter.set(file("${rootProject.projectDir}/config/spotbugs/exclude.xml"))
    }

    // SpotBugs HTML report setup
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
        reports.create("html") {
            required.set(true)
            outputLocation.set(project.layout.buildDirectory.file("reports/spotbugs/${name}.html"))
        }
    }
}

// Create a task to run PMD on all projects
tasks.register("pmdAll") {
    group = "verification"
    description = "Run PMD analysis on all projects"
    dependsOn(subprojects.map { "${it.path}:pmdMain" })
}

// SpotBugs task for all projects
tasks.register("spotbugsAll") {
    group = "verification"
    description = "Run SpotBugs analysis on all projects"
    dependsOn(subprojects.map { "${it.path}:spotbugsMain" })
}