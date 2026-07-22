plugins{
    alias(libs.plugins.oist.application)
}

android {
    namespace = "jp.oist.abcvlib.pidbalancer"
}

val abcvlibProject = rootProject.project(":abcvlib")
val abcvlibDebugClassesJar = tasks.register("jarAbcvlibDebugClasses", org.gradle.jvm.tasks.Jar::class) {
    archiveFileName.set("abcvlib-debug-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/abcvlib-classes"))
    from(abcvlibProject.layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
    dependsOn(":abcvlib:compileDebugKotlin")
}
val abcvlibReleaseClassesJar = tasks.register("jarAbcvlibReleaseClasses", org.gradle.jvm.tasks.Jar::class) {
    archiveFileName.set("abcvlib-release-classes.jar")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/abcvlib-classes"))
    from(abcvlibProject.layout.buildDirectory.dir("tmp/kotlin-classes/release"))
    dependsOn(":abcvlib:compileReleaseKotlin")
}

dependencies {
    implementation(project(":abcvlib"))
    add("debugImplementation", files(abcvlibDebugClassesJar))
    add("releaseImplementation", files(abcvlibReleaseClassesJar))
}

afterEvaluate {
    tasks.named("compileDebugKotlin") {
        dependsOn(abcvlibDebugClassesJar)
        (this as org.jetbrains.kotlin.gradle.tasks.KotlinCompile).libraries.from(
            abcvlibDebugClassesJar.flatMap { it.archiveFile }
        )
    }

    tasks.named("compileReleaseKotlin") {
        dependsOn(abcvlibReleaseClassesJar)
        (this as org.jetbrains.kotlin.gradle.tasks.KotlinCompile).libraries.from(
            abcvlibReleaseClassesJar.flatMap { it.archiveFile }
        )
    }
}
