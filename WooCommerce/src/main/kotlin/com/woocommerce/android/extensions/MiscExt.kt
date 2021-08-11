package com.woocommerce.android.extensions

inline fun <T> T.takeIfNotEqualTo(other: T?, block: (T) -> Unit) {
    if (this != other) block(this)
}

/**
 * Used to convert when statement into expression. In this case compiler check if all the cases are handled
 *
 * when (sealedClass) {
 *
 *      }.exhaustive
 *
 * https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 */
val Any?.exhaustive
    get() = Unit

inline fun <T,X,Z> Pair<T,X>.unwrap(
    action: (T, X) -> Z
) = action(first, second)
