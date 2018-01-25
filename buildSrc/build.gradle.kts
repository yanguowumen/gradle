/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.internal.PropertiesUtils
import java.util.Properties

plugins {
    groovy
    `java-gradle-plugin`
    idea
    eclipse
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

gradlePlugin {
    (plugins) {
        "jsoup" {
            id = "jsoup"
            implementationClass = "org.gradle.plugins.jsoup.JsoupPlugin"
        }
        "buildTypes" {
            id = "build-types"
            implementationClass = "org.gradle.plugins.buildtypes.BuildTypesPlugin"
        }
        "pegdown" {
            id = "pegdown"
            implementationClass = "org.gradle.plugins.pegdown.PegDownPlugin"
        }
    }
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://repo.gradle.org/gradle/libs-snapshots") }
    gradlePluginPortal()
}

dependencies {
    compile("org.ow2.asm:asm:6.0")
    compile("org.ow2.asm:asm-commons:6.0")
    compile(gradleApi())
    compile("com.google.guava:guava-jdk5:14.0.1")
    compile("commons-lang:commons-lang:2.6")
    compile(localGroovy())
    compile("org.codehaus.groovy.modules.http-builder:http-builder:0.7.2")
    testCompile("junit:junit:4.12")
    testCompile("org.spockframework:spock-core:1.0-groovy-2.4")
    testCompile("cglib:cglib-nodep:3.2.5")
    testCompile("org.objenesis:objenesis:2.4")
    testCompile("org.hamcrest:hamcrest-core:1.3")

    compile("org.pegdown:pegdown:1.6.0")
    compile("org.jsoup:jsoup:1.6.3")
    compile("me.champeau.gradle:japicmp-gradle-plugin:0.2.4")
    compile("org.asciidoctor:asciidoctor-gradle-plugin:1.5.6")
    compile("com.github.javaparser:javaparser-core:2.4.0")

    constraints {
        compile("org.codehaus.groovy:groovy-all:2.4.12")
    }
}

val isCiServer: Boolean by extra { System.getenv().containsKey("CI") }

apply {
    from("../gradle/compile.gradle")
    from("../gradle/dependencies.gradle")
    from("../gradle/classycle.gradle")
}

if (!isCiServer || System.getProperty("enableCodeQuality")?.toLowerCase() == "true") {
    apply { from("../gradle/codeQuality.gradle") }
}

apply { from("../gradle/ciReporting.gradle") }

fun readProperties(propertiesFile: File) = Properties().apply {
    propertiesFile.inputStream().use { fis ->
        load(fis)
    }
}

// Workaround caching problems with 'java-gradle-plugin'
// vvvvv
normalization {
    runtimeClasspath {
        ignore("plugin-under-test-metadata.properties")
    }
}

tasks.withType<GeneratePluginDescriptors> {
    doLast {
        outputDirectory.listFiles().forEach { descriptorFile ->
            val descriptorContents = readProperties(descriptorFile)
            descriptorFile.outputStream().use {
                PropertiesUtils.store(descriptorContents, it, null, Charsets.ISO_8859_1, "\n")
            }
        }
    }
}
// ^^^^^

tasks {
    val checkSameDaemonArgs by creating {
        doLast {
            val buildSrcProperties = readProperties(File(project.rootDir, "gradle.properties"))
            val rootProperties = readProperties(File(project.rootDir, "../gradle.properties"))
            val jvmArgs = listOf(buildSrcProperties, rootProperties).map { it.getProperty("org.gradle.jvmargs") }.toSet()
            if (jvmArgs.size > 1) {
                throw GradleException("gradle.properties and buildSrc/gradle.properties have different org.gradle.jvmargs " +
                    "which may cause two daemons to be spawned on CI and in IDEA. " +
                    "Use the same org.gradle.jvmargs for both builds.")
            }
        }
    }

    val build by getting
    build.dependsOn(checkSameDaemonArgs)
}
