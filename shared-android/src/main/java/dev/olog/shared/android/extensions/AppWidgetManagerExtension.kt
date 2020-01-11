package dev.olog.shared.android.extensions

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

fun Context.getAppWidgetsIdsFor(clazz: Class<*>): IntArray {
    return AppWidgetManager.getInstance(this).getAppWidgetIds(ComponentName(this, clazz))
}