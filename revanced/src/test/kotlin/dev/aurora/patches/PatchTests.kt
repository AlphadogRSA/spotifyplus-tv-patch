// SPDX-License-Identifier: GPL-3.0-only
package dev.aurora.patches

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference

private fun fixture(owner: String = "La;", text: String = "[SpotifyTVActivity] was created", registers: Int = 4) =
    ImmutableMethod(owner, "onCreate", listOf(ImmutableMethodParameter("Landroid/os/Bundle;", emptySet(), null)),
        "V", AccessFlags.PUBLIC.value, emptySet(), emptySet(),
        ImmutableMethodImplementation(registers, listOf(
            ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(text)),
            ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList()))

fun main() {
    var count = 0
    fun rejects(action: () -> Unit) {
        check(runCatching(action).isFailure); count++
    }
    val original = fixture("Loriginal/Name;")
    val renamed = fixture("Lobfuscated/Z;")
    check(Fingerprints.create.matches(original)); count++
    check(Fingerprints.create.matches(renamed)); count++
    check(!Fingerprints.create.matches(fixture(text = "wrong marker"))); count++
    check(uniqueMethod(listOf(original), Fingerprints.create) == original); count++
    rejects { uniqueMethod(emptyList(), Fingerprints.create) }
    rejects { uniqueMethod(listOf(original, renamed), Fingerprints.create) }
    check(parameterRegister(original, 0) == 2); count++
    rejects { requireScratchLocal(fixture(registers = 2)) }
    requireScratchLocal(original); count++
    rejects { ensureFreshTypes(listOf("Ldev/aurora/tvpatch/Hooks;")) }
    ensureFreshTypes(listOf("Loriginal/App;")); count++
    val methods = listOf(original, fixture("Lother;", "not this method"))
    check(uniqueMethod(methods, Fingerprints.create).definingClass == original.definingClass); count++
    fun adFixture(static: Boolean, marker: String = "is_ad_playing") = ImmutableMethod("Lrenamed/Router;", "renamedCallback",
        listOf("Lrenamed/Router;", "Ljava/lang/String;", "I", "Ljava/lang/String;").map { ImmutableMethodParameter(it, emptySet(), null) },
        "Lrenamed/Result;", AccessFlags.PRIVATE.value or if (static) AccessFlags.STATIC.value else 0, emptySet(), emptySet(),
        ImmutableMethodImplementation(10, listOf(marker, "duration_ms", "is_shuffled").map {
            ImmutableInstruction21c(Opcode.CONST_STRING, 0, ImmutableStringReference(it))
        } + ImmutableInstruction11x(Opcode.RETURN_OBJECT, 0), emptyList(), emptyList()))
    check(Fingerprints.ad.matches(adFixture(true))); count++
    check(!Fingerprints.ad.matches(adFixture(false))); count++
    check(!Fingerprints.ad.matches(adFixture(true, "different_boolean"))); count++
    rejects { uniqueMethod(listOf(adFixture(true), adFixture(true)), Fingerprints.ad) }
    println("Fingerprint/safety tests: $count checks passed")
}
