package com.learnhuayu.app.ui.session

import com.learnhuayu.core.model.DrillMode

/**
 * Which kind of spec a session route points at. The route carries the lowercase
 * [routeValue]; lessons always browse, practice uses the spec's [DrillMode].
 */
enum class SessionKind(val routeValue: String) {
    LESSON("lesson"),
    PRACTICE("practice"),
    ;

    companion object {
        fun fromRoute(value: String): SessionKind? = entries.firstOrNull { it.routeValue == value }
    }
}
