plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.originps"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("com.originps.launcher.Main")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("OriginLauncher")
    archiveClassifier.set("")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "com.originps.launcher.Main"
    }
}

tasks.named("build") {
    dependsOn("installDesktop")
}

tasks.register<Copy>("stageDesktop") {
    dependsOn("shadowJar")
    from(tasks.named("shadowJar"))
    from("config.json")
    into(layout.buildDirectory.dir("desktop"))
    rename { if (it.endsWith(".jar")) "OriginLauncher.jar" else it }
}

// jpackage copies every file in its input directory into the app bundle.
// Keep config.json beside the launcher JAR so installed apps use the same
// settings as the standalone JAR.
tasks.register<Copy>("stageNativeInput") {
    dependsOn("shadowJar")
    from(tasks.named("shadowJar"))
    from("config.json")
    into(layout.buildDirectory.dir("native-input"))
    rename { if (it.endsWith(".jar")) "OriginLauncher.jar" else it }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.register<Copy>("installDesktop") {
    dependsOn("stageDesktop")
    from(layout.buildDirectory.dir("desktop"))
    into(projectDir)
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register<Exec>("jpackageNative") {
    dependsOn("stageNativeInput")

    doFirst {
        val outDir = layout.buildDirectory.dir("native").get().asFile
        delete(outDir)
        mkdir(outDir)

        val inputDir = layout.buildDirectory.dir("native-input").get().asFile

        val isMac = System.getProperty("os.name").lowercase().contains("mac")
        val args = mutableListOf(
            "jpackage",
            "--name", "OriginPS",
            "--input", inputDir.absolutePath,
            "--main-jar", "OriginLauncher.jar",
            "--main-class", "com.originps.launcher.Main",
            "--app-version", project.version.toString(),
            "--dest", outDir.absolutePath
        )

        if (isMac) {
            args.addAll(listOf("--type", "dmg", "--mac-package-name", "OriginPS"))
        } else {
            args.addAll(listOf("--type", "app-image"))
        }

        commandLine(args)
    }
}
