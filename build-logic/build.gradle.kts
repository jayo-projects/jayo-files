plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("optionalDependenciesPlugin") {
            id = "jayo.build.optional-dependencies"
            implementationClass = "jayo.build.OptionalDependenciesPlugin"
        }
    }
}
