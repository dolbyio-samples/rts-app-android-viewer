package io.dolby.rtscomponentkit.data.multistream

enum class VideoQuality {
    AUTO, LOW, MEDIUM, HIGH;

    fun lower(): VideoQuality {
        return when (this) {
            HIGH -> MEDIUM
            MEDIUM -> LOW
            LOW -> AUTO
            AUTO -> AUTO
        }
    }

    companion object {
        fun valueToQuality(value: String?): VideoQuality {
            return try {
                value?.let {
                    VideoQuality.valueOf(it.uppercase())
                } ?: run { default }
            } catch (ex: Exception) {
                default
            }
        }

        val default: VideoQuality = AUTO
    }
}
