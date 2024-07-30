import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
import java.nio.charset.StandardCharsets
import kotlin.jvm.optionals.getOrNull
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

println("Using Gradle version: ${gradle.gradleVersion}")
println("Using Kotlin compiler version: $KOTLIN_VERSION")
println("Using Java compiler version: ${JavaVersion.current()}")

plugins {
    `maven-publish`
    signing
    alias(libs.plugins.release)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    id("jayo.build.optional-dependencies")
}

val versionCatalog: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun catalogVersion(lib: String) =
    versionCatalog.findVersion(lib).getOrNull()?.requiredVersion
        ?: throw GradleException("Version '$lib' is not specified in the toml version catalog")

val javaVersion = catalogVersion("java").toInt()

kotlin {
    compilerOptions {
        languageVersion.set(KOTLIN_2_0)
        apiVersion.set(KOTLIN_2_0)
        javaParameters = true
        allWarningsAsErrors = true
        explicitApi = Strict
        freeCompilerArgs.addAll(
            "-Xjvm-default=all",
            "-Xnullability-annotations=@org.jspecify.annotations:strict" // not really sure if this helps ;)
        )
    }

    jvmToolchain(javaVersion)
}

repositories {
    mavenCentral()
}

dependencies {
    api("dev.jayo:jayo:${catalogVersion("jayo")}")

    compileOnly("org.jspecify:jspecify:${catalogVersion("jspecify")}")

    optional("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation(platform("org.junit:junit-bom:${catalogVersion("junit")}"))
    testImplementation("org.assertj:assertj-core:${catalogVersion("assertj")}")
    testImplementation("org.junit.jupiter:junit-jupiter-api")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.slf4j:slf4j-simple:${catalogVersion("slf4j")}")
    testRuntimeOnly("org.slf4j:slf4j-jdk-platform-logging:${catalogVersion("slf4j")}")
}

kover {
    reports {
        total {
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
}

tasks {
    withType<JavaCompile> {
        options.encoding = StandardCharsets.UTF_8.toString()
        options.release = javaVersion

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
                description.set("Jayo Files is a file library based on Jayo for the JVM")
                url.set("https://github.com/jayo-projects/jayo-files")

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
                    connection.set("scm:git:https://github.com/jayo-projects/jayo-files")
                    developerConnection.set("scm:git:git://github.com/jayo-projects/jayo-files.git")
                    url.set("https://github.com/jayo-projects/jayo-files.git")
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
    gradleVersion = "8.9"
    distributionType = Wrapper.DistributionType.ALL
}
