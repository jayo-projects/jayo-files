import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.charset.StandardCharsets
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

println("Using Gradle version: ${gradle.gradleVersion}")
println("Using Kotlin compiler version: $KOTLIN_VERSION")
println("Using Java compiler version: ${JavaVersion.current()}")

plugins {
    id("jayo.build.optional-dependencies")
    kotlin("jvm")
    id("org.jetbrains.dokka")
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.kotlinx.kover")
    id("net.researchgate.release")
}

repositories {
    mavenCentral()
}

dependencies {
    api("dev.jayo:jayo:${property("jayoVersion")}")

    compileOnly("org.jspecify:jspecify:${property("jspecifyVersion")}")

    optional("org.jetbrains.kotlin:kotlin-stdlib")
    
    testImplementation(platform("org.junit:junit-bom:${property("junitVersion")}"))
    testImplementation("org.assertj:assertj-core:${property("assertjVersion")}")
    testImplementation("org.junit.jupiter:junit-jupiter-api")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.slf4j:slf4j-simple:${property("slf4jVersion")}")
}

kotlin {
    explicitApi()
    jvmToolchain(21)
}

koverReport {
    defaults {
        verify {
            onCheck = true
            rule {
                bound {
                    minValue = 0
                }
            }
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = StandardCharsets.UTF_8.toString()
        options.release = 21

        // replace '-' with '.' to match JPMS jigsaw module name
        val jpmsName = project.name.replace('-', '.')
        // this is needed because we have a separate compile step because the Java code is in 'main/java' and the Kotlin
        // code is in 'main/kotlin'
        options.compilerArgs.addAll(
            listOf(
                "--patch-module",
                "$jpmsName=${sourceSets.main.get().output.asPath}",
            )
        )
    }

    withType<KotlinCompile> {
        kotlinOptions {
            languageVersion = "1.8" // switch to "2.0" with K2 compiler when stable
            apiVersion = "1.8" // switch to "2.0" with K2 compiler when stable
            javaParameters = true
            allWarningsAsErrors = true
            freeCompilerArgs += arrayOf(
                "-Xjvm-default=all",
                "-Xnullability-annotations=@org.jspecify.annotations:strict" // not really sure if this helps ;)
            )
        }
    }

    withType<Jar> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }
}

// --------------- publishing ---------------

// Generate and add javadoc and html-doc jars in jvm artefacts
val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

java {
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repos/releases"))
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)
            
            pom {
                name.set(project.name)
                description.set("Jayo is a synchronous I/O library for the JVM")
                url.set("https://github.com/jayo-projects/jayo")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        name.set("pull-vert")
                        url.set("https://github.com/pull-vert")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/jayo-projects/jayo")
                    developerConnection.set("scm:git:git://github.com/jayo-projects/jayo.git")
                    url.set("https://github.com/jayo-projects/jayo.git")
                }
            }
        }
    }
}

signing {
    // Require signing.keyId, signing.password and signing.secretKeyRingFile
    sign(publishing.publications)
}

// when version changes :
// -> execute ./gradlew wrapper, then remove .gradle directory, then execute ./gradlew wrapper again
tasks.wrapper {
    gradleVersion = "8.5"
    distributionType = Wrapper.DistributionType.ALL
}
