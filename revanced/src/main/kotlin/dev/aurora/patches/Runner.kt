// SPDX-License-Identifier: GPL-3.0-only
package dev.aurora.patches

import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.patch.loadPatches
import app.revanced.patcher.patcher
import java.io.File

fun main(args: Array<String>) {
    require(args.size >= 2) { "Usage: list BUNDLE | apply BUNDLE INPUT_APK OUTPUT_APK NEW_WORK_DIRECTORY AAPT2 [PATCH_NAME ...]" }
    val bundle = File(args[1]).canonicalFile
    require(bundle.isFile) { "Patch bundle not found: $bundle" }
    val patches = loadPatches(bundle) { file, error -> throw IllegalStateException("Could not load $file", error) }
    require(patches.isNotEmpty()) { "No patches found in bundle" }
    if (args[0] == "list") {
        patches.sortedBy { it.name }.forEach { patch ->
            println("${patch.name} | default=${patch.use} | dependencies=${patch.dependencies.joinToString { it.name ?: "internal" }} | compatible=${patch.compatiblePackages}")
        }
        return
    }
    require(args[0] == "apply" && args.size >= 6) { "Invalid apply arguments" }
    val input = File(args[2]).canonicalFile
    val output = File(args[3]).canonicalFile
    val work = File(args[4]).canonicalFile
    val aapt = File(args[5]).canonicalFile
    require(input.isFile && aapt.isFile) { "Input APK and AAPT2 must exist" }
    require(!output.exists() && !work.exists()) { "Output and working directory must be new; refusing to overwrite or delete existing paths" }
    val selectedNames = args.drop(6).toSet()
    val knownNames = patches.map { it.name }.toSet()
    require(selectedNames.all { it in knownNames }) { "Unknown patch selection: ${selectedNames - knownNames}" }
    val selected = patches.filter { if (selectedNames.isEmpty()) it.use else it.name in selectedNames }.toSet()
    require(selected.isNotEmpty()) { "No patches selected" }
    val failures = mutableListOf<String>()
    val applyPatches = patcher(input, work, aapt, File(work, "framework").absolutePath) { packageName, version ->
        for (patch in selected) require(patch.compatiblePackages?.any { (name, versions) -> name == packageName && (versions == null || version in versions) } != false) {
            "${patch.name} is not compatible with $packageName $version"
        }
        selected
    }
    val result = applyPatches { status ->
        if (status.exception != null) failures += "${status.patch}: ${status.exception!!.message}"
        println("${status.patch}: ${if (status.exception == null) "OK" else "FAILED"}")
    }
    check(failures.isEmpty()) { failures.joinToString("\n") }
    output.parentFile.mkdirs()
    input.copyTo(output)
    try { result.applyTo(output) }
    catch (error: Throwable) { output.delete(); throw error }
    println("Prepared unsigned patched APK: $output")
}
