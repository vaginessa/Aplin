package com.nagopy.android.aplin

import android.view.View

fun <E : View> E.visible() {
    visibility = View.VISIBLE
}

fun <E : View> E.invisible() {
    visibility = View.INVISIBLE
}

fun <E : View> E.gone() {
    visibility = View.GONE
}

inline fun <T> Iterable<T>.forEachX(first: (first: T) -> Unit, each: (current: T, previous: T) -> Unit) {
    var prev: T
    this.forEachIndexed { i, t ->
        if (i == 0) {
            first(t)
        } else {
            each(t, prev)
        }
        prev = t
    }
}
