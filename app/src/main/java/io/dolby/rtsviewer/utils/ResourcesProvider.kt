package io.dolby.rtsviewer.utils

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ResourcesProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }
}
