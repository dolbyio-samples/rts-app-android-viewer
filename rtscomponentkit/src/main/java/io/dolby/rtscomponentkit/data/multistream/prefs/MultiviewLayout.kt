package io.dolby.rtscomponentkit.data.multistream.prefs

import io.dolby.rtscomponentkit.R

enum class MultiviewLayout {
    ListView, SingleStreamView, GridView;

    fun stringResource(): Int {
        return when (this) {
            ListView -> R.string.settings_multiview_layout_list_view
            SingleStreamView -> R.string.settings_multiview_layout_single_view
            GridView -> R.string.settings_multiview_layout_grid_view
        }
    }
    companion object {
        fun valueToLayout(value: String): MultiviewLayout {
            return try {
                MultiviewLayout.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }
        val default: MultiviewLayout = ListView
    }
}
