package io.dolby.interactiveplayer.rts.data

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
        fun valueToQuality(value: String): VideoQuality {
            return try {
                VideoQuality.valueOf(value)
            } catch (ex: Exception) {
                default
            }
        }

        val default: VideoQuality = AUTO
    }
}