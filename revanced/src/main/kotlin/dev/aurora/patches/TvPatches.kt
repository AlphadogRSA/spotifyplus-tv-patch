// SPDX-License-Identifier: GPL-3.0-only
@file:Suppress("unused")
package dev.aurora.patches

import app.revanced.patcher.extensions.*
import app.revanced.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import org.w3c.dom.Element

private const val PACKAGE = "com.spotify.tv.android"
private const val VERSION = "1.134.2"

val tvManifestPatch = resourcePatch(name = "TV app label and local lyric import", use = false) {
    compatibleWith(PACKAGE(VERSION))
    apply {
        document("AndroidManifest.xml").use { doc ->
            val app = doc.getElementsByTagName("application").item(0) as Element
            val android = "http://schemas.android.com/apk/res/android"
            app.setAttributeNS(android, "android:label", "Spotify+ TV")
            doc.documentElement.setAttributeNS(android, "android:versionName", "1.3.0.1")
            val provider = doc.createElement("provider")
            provider.setAttributeNS(android, "android:name", "dev.aurora.tvpatch.hush.HelperProvider")
            provider.setAttributeNS(android, "android:authorities", "com.spotify.tv.android.sptv_hush_helper")
            provider.setAttributeNS(android, "android:exported", "true")
            provider.setAttributeNS(android, "android:permission", "android.permission.INTERACT_ACROSS_USERS_FULL")
            app.appendChild(provider)
            app.setAttributeNS(android, "android:debuggable", "true")
            val activities = doc.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val node = activities.item(i) as Element
                if (node.getAttributeNS(android, "name") == "com.spotify.tv.android.SpotifyTVActivity")
                    node.setAttributeNS(android, "android:label", "Spotify+ TV")
            }
            for (permission in listOf("android.permission.MODIFY_AUDIO_SETTINGS", "android.permission.DUMP")) {
                if ((0 until doc.getElementsByTagName("uses-permission").length).none {
                    (doc.getElementsByTagName("uses-permission").item(it) as Element).getAttributeNS(android, "name") == permission
                }) {
                    val element = doc.createElement("uses-permission")
                    element.setAttributeNS(android, "android:name", permission)
                    doc.documentElement.insertBefore(element, app)
                }
            }
        }
    }
}

val tvAssetsPatch = rawResourcePatch(name = "TV theme assets and notices", use = false) {
    compatibleWith(PACKAGE(VERSION))
    apply {
        for (name in listOf("spotifyplus-tv.js", "TV-PATCH-LICENSE.txt", "SPOTIFYPLUS-LICENSE.txt", "INSPIRATION.md", "spotifyplus-settings.js", "HUSH-NOTICE.txt", "HUSH-COPYING.txt")) {
            val target = get("assets/$name", false)
            target.parentFile.mkdirs()
            requireNotNull(object {}.javaClass.classLoader.getResourceAsStream(name)) { "Missing bundled asset $name" }.use { input ->
                target.outputStream().use(input::copyTo)
            }
        }
    }
}

private val preflightPatch = bytecodePatch(name = "TV fingerprint preflight", use = false) {
    compatibleWith(PACKAGE(VERSION))
    apply {
        ensureFreshTypes(classDefs.map { it.type })
        val create = resolve(Fingerprints.create)
        require(resolve(Fingerprints.destroy).definingClass == create.definingClass) { "Lifecycle methods matched different activities" }
        requireScratchLocal(resolve(Fingerprints.keys(create.definingClass)))
        resolve(Fingerprints.page)
        resolve(Fingerprints.session)
        resolve(Fingerprints.ad)
        resolve(Fingerprints.volume)
        val playback = resolve(Fingerprints.playback)
        require(classDefs[playback.definingClass]!!.fields.count { it.name == "bridge" && it.type.endsWith("/TVBridgeApi;") } == 1)
    }
}

private val runtimePatch = bytecodePatch(name = "TV runtime extension", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(preflightPatch)
    extendWith("extensions/spotifyplus-tv.rve")
}

val tvLifecyclePatch = bytecodePatch(name = "TV lifecycle hooks", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(runtimePatch)
    apply {
        val create = resolve(Fingerprints.create)
        val returns = create.implementation!!.instructions.withIndex().filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        require(returns.isNotEmpty()) { "No normal lifecycle exit found" }
        for (index in returns.asReversed()) create.addInstruction(index, "invoke-static/range {p0 .. p0}, $HOOK->attach(Landroid/app/Activity;)V")
        resolve(Fingerprints.destroy).addInstruction(0, "invoke-static/range {p0 .. p0}, $HOOK->detach(Landroid/app/Activity;)V")
    }
}

val tvRemotePatch = bytecodePatch(name = "TV remote settings menu", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(tvLifecyclePatch, tvAssetsPatch, tvManifestPatch)
    apply {
        val create = resolve(Fingerprints.create)
        val keys = resolve(Fingerprints.keys(create.definingClass))
        requireScratchLocal(keys)
        keys.addInstructionsWithLabels(0, """
            invoke-static/range {p0 .. p1}, $HOOK->key(Landroid/app/Activity;Landroid/view/KeyEvent;)Z
            move-result v0
            if-eqz v0, :original
            const/4 v0, 0x1
            return v0
        """.trimIndent(), ExternalLabel("original", keys.getInstruction(0)))
    }
}

val tvWebViewPatch = bytecodePatch(name = "TV WebView theme hook", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(tvLifecyclePatch, tvAssetsPatch)
    apply {
        resolve(Fingerprints.page).addInstruction(0, "invoke-static/range {p1 .. p1}, $HOOK->page(Landroid/webkit/WebView;)V")
        var count = 0
        for (definition in classDefs.filter { it.type.startsWith("Lcom/spotify/tv/") }) {
            val candidates = definition.methods.filter { method ->
                method.implementation?.instructions?.any {
                    val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.definingClass == "Landroid/webkit/WebView;" && ref.name == "loadUrl"
                } == true
            }
            if (candidates.isEmpty()) continue
            val mutable = classDefs.getOrReplaceMutable(definition)
            for (original in candidates) {
                val method = mutable.methods.single { it.name == original.name && it.parameterTypes == original.parameterTypes && it.returnType == original.returnType }
                val calls = method.implementation!!.instructions.withIndex().filter {
                    val ref = (it.value as? ReferenceInstruction)?.reference as? MethodReference
                    ref?.definingClass == "Landroid/webkit/WebView;" && ref.name == "loadUrl"
                }
                for (call in calls.asReversed()) {
                    val register = (call.value as? FiveRegisterInstruction)?.registerC
                        ?: (call.value as RegisterRangeInstruction).startRegister
                    method.addInstruction(call.index, "invoke-static/range {v$register .. v$register}, $HOOK->beforeLoad(Landroid/webkit/WebView;)V")
                    count++
                }
            }
        }
        require(count == 2) { "Expected two verified pre-navigation WebView loads, found $count" }
    }
}

val tvSessionPatch = bytecodePatch(name = "TV own-session lyrics and timer", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(runtimePatch)
    apply {
        val method = resolve(Fingerprints.session)
        val assignment = sessionAssignments(method).single()
        val register = (assignment.value as TwoRegisterInstruction).registerA
        method.addInstruction(assignment.index + 1, "invoke-static/range {v$register .. v$register}, $HOOK->bindSession(Ljava/lang/Object;)V")
    }
}

val tvAdMutePatch = bytecodePatch(name = "TV detected-ad muting", use = false) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(runtimePatch)
    apply {
        val method = resolve(Fingerprints.ad)
        resolve(Fingerprints.playback).addInstruction(0, "invoke-static/range {p0 .. p3}, $HOOK->playbackEvent(Ljava/lang/Object;III)V")
        val instructions = method.implementation!!.instructions.toList()
        val marker = instructions.withIndex().single { ((it.value as? ReferenceInstruction)?.reference as? StringReference)?.string == "is_ad_playing" }.index
        val call = instructions[marker + 1] as ReferenceInstruction
        val ref = call.reference as MethodReference
        require(ref.definingClass == "Lorg/json/JSONObject;" && ref.name == "getBoolean")
        require(instructions[marker + 2].opcode == Opcode.MOVE_RESULT)
        val register = (instructions[marker + 2] as OneRegisterInstruction).registerA
        method.addInstruction(marker + 3, "invoke-static/range {v$register .. v$register}, $HOOK->adState(Z)V")
        resolve(Fingerprints.volume).addInstructions(0, """
            invoke-static/range {p0 .. p1}, $HOOK->filterVolume(Ljava/lang/Object;I)I
            move-result p1
        """.trimIndent())
    }
}

val spotifyPlusTvPatch = bytecodePatch(
    name = "SpotifyPlus TV interface and lyrics",
    description = "TV remote menu, themes, local/LRCLIB lyrics and own-session sleep timer. No account entitlement changes.",
) {
    compatibleWith(PACKAGE(VERSION))
    dependsOn(tvRemotePatch, tvWebViewPatch, tvSessionPatch, tvAdMutePatch)
}
