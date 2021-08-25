package com.inapp.update

import android.content.Context
import android.content.SharedPreferences

class AppConfig(context: Context) {

    companion object {
        private const val UPDATE_NEEDED = "UPDATE_NEEDED"
    }

    var sharedPreferences: SharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    var updateNeeded:Boolean
        get() = sharedPreferences.getBoolean(UPDATE_NEEDED, true)
        set(value) = sharedPreferences.edit().putBoolean(UPDATE_NEEDED, value).apply()
}