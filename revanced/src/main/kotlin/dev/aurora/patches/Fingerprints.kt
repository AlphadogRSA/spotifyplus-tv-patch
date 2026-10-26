// SPDX-License-Identifier: GPL-3.0-only
package dev.aurora.patches

import app.revanced.com.android.tools.smali.dexlib2.mutable.MutableMethod
import app.revanced.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

const val HOOK = "Ldev/aurora/tvpatch/Hooks;"
const val SESSION = "Landroid/support/v4/media/session/MediaSessionCompat;"

data class MethodFingerprint(
    val label: String,
    val returnType: String?,
    val parameters: List<String>?,
    val name: String? = null,
    val strings: Set<String> = emptySet(),
    val isStatic: Boolean = false,
    val extra: (Method) -> Boolean = { true },
) {
    fun matches(method: Method): Boolean {
        if (method.implementation == null || (method.accessFlags and AccessFlags.STATIC.value != 0) != isStatic) return false
        if (returnType != null && method.returnType != returnType) return false
        if (parameters != null && method.parameterTypes.map { it.toString() } != parameters) return false
        if (name != null && method.name != name) return false
        val present = method.implementation!!.instructions.mapNotNull { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string }.toSet()
        return present.containsAll(strings) && extra(method)
    }
}

object Fingerprints {
    val playback = MethodFingerprint("native playback volume events", "V", null,
        strings = setOf("[SpotifyTVService] handlePlaybackEvent: ", "[onPlaybackEvent] Received first volume event Volume ("), isStatic = true) {
        it.parameterTypes.size == 4 && it.parameterTypes.drop(1).map { p -> p.toString() } == listOf("I", "I", "I")
    }
    val ad = MethodFingerprint("native ad state", null, null, strings = setOf("is_ad_playing", "duration_ms", "is_shuffled"), isStatic = true) {
        it.parameterTypes.size == 4 && it.parameterTypes.drop(1).map { p -> p.toString() } == listOf("Ljava/lang/String;", "I", "Ljava/lang/String;")
    }
    val volume = MethodFingerprint("player-local volume", "V", listOf("I"), "setVolume") { method ->
        method.implementation!!.instructions.any {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == method.definingClass && ref.name == "applyVolume" && ref.parameterTypes.map { p -> p.toString() } == listOf("I") && ref.returnType == "V"
        }
    }
    val create = MethodFingerprint("activity creation", "V", listOf("Landroid/os/Bundle;"), "onCreate", setOf("[SpotifyTVActivity] was created"))
    val destroy = MethodFingerprint("activity destruction", "V", emptyList(), "onDestroy", setOf("[SpotifyTVActivity] started to destroy"))
    fun keys(owner: String) = MethodFingerprint("remote key callback", "Z", listOf("Landroid/view/KeyEvent;"), "dispatchKeyEvent", setOf("event")) { it.definingClass == owner }
    val page = MethodFingerprint("WebView page completion", "V", listOf("Landroid/webkit/WebView;", "Ljava/lang/String;"), "onPageFinished", setOf("[SpotifyTVPresenterImpl] Executing pending javascript intent"))
    val session = MethodFingerprint("own media-session creation", "V", emptyList(), strings = setOf("spotify-android-tv-media-session")) { method ->
        method.implementation!!.instructions.any {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == SESSION && ref.name == "<init>"
        } && sessionAssignments(method).size == 1
    }
}

fun uniqueMethod(methods: Iterable<Method>, fingerprint: MethodFingerprint): Method {
    val matches = methods.filter(fingerprint::matches)
    require(matches.size == 1) { "${fingerprint.label}: expected exactly one match; found ${matches.size}. No method-name fallback is allowed." }
    return matches.single()
}

fun BytecodePatchContext.resolve(fingerprint: MethodFingerprint): MutableMethod {
    val match = uniqueMethod(classDefs.flatMap { it.methods }, fingerprint)
    val owner = requireNotNull(classDefs[match.definingClass])
    return classDefs.getOrReplaceMutable(owner).methods.single {
        it.name == match.name && it.returnType == match.returnType && it.parameterTypes == match.parameterTypes
    }
}

fun parameterRegister(method: Method, parameterIndex: Int): Int {
    require(method.accessFlags and AccessFlags.STATIC.value == 0) { "Instance method required" }
    val words = method.parameterTypes.sumOf { if (it.toString() in setOf("J", "D")) 2 else 1 }
    val base = requireNotNull(method.implementation).registerCount - words - 1
    require(parameterIndex in 0..words)
    return base + parameterIndex
}

fun requireScratchLocal(method: Method) {
    require(parameterRegister(method, 0) >= 1) { "Remote callback has no local register; refusing to overwrite its parameters" }
}

fun ensureFreshTypes(types: Iterable<String>) {
    require(types.none { it.startsWith("Ldev/aurora/tvpatch/") }) { "This APK already contains the TV extension. Patch the original APK instead." }
}

fun sessionAssignments(method: Method) = method.implementation!!.instructions.withIndex().filter {
    val field = ((it.value as? ReferenceInstruction)?.reference as? FieldReference)
    it.value.opcode == Opcode.IPUT_OBJECT && field?.type == SESSION
}
