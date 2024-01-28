package io.dolby.interactiveplayer.rts.data

enum class VideoQuality(val lower: VideoQuality) {
    AUTO(AUTO),
    LOW(AUTO),
    MEDIUM(LOW),
    HIGH(MEDIUM);

    companion object {
        fun valueToQuality(value: String) = try {
            VideoQuality.valueOf(value)
        } catch (ex: IllegalArgumentException) {
            default
        }

        val default: VideoQuality = AUTO
    }
}