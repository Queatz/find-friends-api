package org.morefriends

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.*
import kotlin.random.Random

internal val emailAddressPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()

fun String.isPhoneNumber() = try { PhoneNumberUtil.getInstance().isValidNumber(PhoneNumberUtil.getInstance().parse(this, Locale.US.country)) } catch (e: NumberParseException) { false }
fun String.normalizePhoneNumber() = PhoneNumberUtil.getInstance().format(PhoneNumberUtil.getInstance().parse(this, Locale.US.country), PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)!!
fun String.isEmailAddress() = this.matches(emailAddressPattern)

fun IntRange.token() = joinToString("") { Random.nextInt(35).toString(36) }
