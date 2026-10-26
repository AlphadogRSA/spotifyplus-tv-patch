plugins { kotlin("jvm") version "2.3.10" }

group = "dev.aurora.tvpatch"
version = "0.5.0"
kotlin {
    jvmToolchain(17)
    // The pinned official Patcher is built with experimental context/backing-field metadata.
    compilerOptions { freeCompilerArgs.addAll("-Xcontext-parameters", "-Xskip-prerelease-check") }
}

val cliJar = layout.projectDirectory.file(".tools/revanced-cli-6.0.0-all.jar")
val prepareTools by tasks.registering(Exec::class) {
    commandLine("python", "bootstrap.py")
    inputs.file("bootstrap.py")
    outputs.file(cliJar)
}
dependencies { compileOnly(files(cliJar)); testImplementation(files(cliJar)) }
tasks.named("compileKotlin") { dependsOn(prepareTools) }

sourceSets.main {
    resources.srcDir("../assets")
}
val extensionDex = providers.gradleProperty("extensionDex")
tasks.processResources {
    if (extensionDex.isPresent) {
        from(extensionDex) { into("extensions"); rename { "spotifyplus-tv.rve" } }
    }
}
val bundle by tasks.registering(Jar::class) {
    dependsOn(tasks.classes)
    archiveFileName.set("spotifyplus-tv-0.5.rvp")
    from(sourceSets.main.get().output)
    doFirst { require(extensionDex.isPresent && file(extensionDex.get()).isFile) { "Pass -PextensionDex=/absolute/path/to/classes.dex from the compiled MIT runtime extension" } }
    manifest { attributes("Main-Class" to "dev.aurora.patches.RunnerKt") }
}
tasks.register<JavaExec>("testFingerprints") {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("dev.aurora.patches.PatchTestsKt")
}
