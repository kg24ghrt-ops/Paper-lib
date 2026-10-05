plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "dev.paperlib"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass = "dev.paperlib.cli.MainKt"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

/**
 * Single runnable JAR with the Kotlin stdlib folded in, so the CLI can be
 * executed with `java -jar` without any classpath setup.
 */
val runnableJar by tasks.registering(Jar::class) {
    archiveClassifier = "all"
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class")
}

tasks.named("build") {
    dependsOn(runnableJar)
}