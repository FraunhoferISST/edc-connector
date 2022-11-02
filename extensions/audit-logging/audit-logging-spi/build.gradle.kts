/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

val mockitoVersion: String by project

plugins {
    `java-library`
}
dependencies {
    api(project(":spi:common:core-spi"))
    api(project(":spi:control-plane:control-plane-spi"))
    implementation(project(":core"))
}


publishing {
    publications {
        create<MavenPublication>("audit-logging-spi") {
            artifactId = "audit-logging-spi"
            from(components["java"])
        }
    }
}