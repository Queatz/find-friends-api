package org.morefriends

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.morefriends.models.Choice
import java.util.*
import kotlin.random.Random

internal val emailAddressPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()

fun String.isPhoneNumber() = try { PhoneNumberUtil.getInstance().isValidNumber(PhoneNumberUtil.getInstance().parse(this, Locale.US.country)) } catch (e: NumberParseException) { false }
fun String.normalizePhoneNumber() = PhoneNumberUtil.getInstance().format(PhoneNumberUtil.getInstance().parse(this, Locale.US.country), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)!!
fun String.isEmailAddress() = this.matches(emailAddressPattern)

fun IntRange.token() = joinToString("") { Random.nextInt(35).toString(36) }

fun <T : Any> List<T>.emptyOrHas(other: T?) = (isEmpty() && other == null) || any { it == other }
fun <T : Any> List<T>.emptyOrHasAny(other: List<T>) = (isEmpty() && other.isEmpty()) || any { other.contains(it) }
fun <T : Any> List<T>.emptyOrHasAll(other: List<T>) = (isEmpty() && other.isEmpty()) || all { other.contains(it) }

fun Map<String, Choice>.anyAreRequiredAndNotEqual(other: Map<String, Choice>) = entries.any {
    it.value.required == true && other[it.key]?.choice != it.value.choice
}
