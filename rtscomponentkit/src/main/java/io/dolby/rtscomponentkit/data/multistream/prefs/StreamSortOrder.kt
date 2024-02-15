package io.dolby.rtscomponentkit.data.multistream.prefs

import io.dolby.rtscomponentkit.R

enum class StreamSortOrder {
    ConnectionOrder, AlphaNumeric;

    fun stringResource(): Int {
        return when (this) {
            ConnectionOrder -> R.string.settings_stream_sort_order_connection
            AlphaNumeric -> R.string.settings_stream_sort_order_alphanumeric
        }
    }

    companion object {
        fun valueToSortOrder(value: String): StreamSortOrder {
            return try {
                StreamSortOrder.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }

        val default: StreamSortOrder = ConnectionOrder
    }
}
