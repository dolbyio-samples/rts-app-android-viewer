package io.dolby.rtscomponentkit.data

import org.webrtc.RTCStatsReport
import java.math.BigInteger

class StatisticsData(
    val roundTripTime: Double?,
    val timestamp: Double?,
    val audio: StatsInboundRtp?,
    val video: StatsInboundRtp?
) {
    companion object {
        fun from(report: RTCStatsReport): StatisticsData {
            val rtt: Double? = getStatisticsRoundTripTime(report)
            var audio: StatsInboundRtp? = null
            var video: StatsInboundRtp? = null

            report.statsMap.values.forEach { statsData ->
                if (statsData.type == "inbound-rtp") {
                    val statsMembers = statsData.members
                    val codecId = statsMembers["codecId"] as String
                    val codecName = getStatisticsCodec(codecId, report)

                    val trackId = statsMembers["trackId"] as String

                    val statsInboundRtp = StatsInboundRtp(
                        sid = statsMembers["id"] as String?,
                        kind = statsMembers["kind"] as String,
                        decoder = statsMembers.getOrDefault(
                            "decoderImplementation",
                            null
                        ) as String?,
                        frameWidth = statsMembers["frameWidth"] as Long?,
                        frameHeight = statsMembers["frameHeight"] as Long?,
                        fps = statsMembers.getOrDefault("framesPerSecond", null) as Double?,
                        audioLevel = statsMembers.getOrDefault("audioLevel", null) as Double?,
                        totalEnergy = statsMembers.getOrDefault(
                            "totalAudioEnergy",
                            null
                        ) as Double?,
                        framesReceived = statsMembers.getOrDefault("framesReceived", null) as Int?,
                        framesDecoded = statsMembers.getOrDefault("framesDecoded", null) as Long?,
                        nackCount = statsMembers.getOrDefault("nackCount", null) as Long?,
                        bytesReceived = statsMembers.get("bytesReceived") as BigInteger,
                        totalSampleDuration = statsMembers.getOrDefault(
                            "totalSamplesDuration",
                            null
                        ) as Double?,
                        codecId = codecId,
                        jitter = statsMembers["jitter"] as Double,
                        packetsReceived = statsMembers["packetsReceived"] as Long,
                        packetsLost = statsMembers["packetsLost"] as Int,
                        timestamp = statsMembers["timestampUs"] as Long?,
                        codecName = codecName
                    )
                    if (statsInboundRtp.isVideo) {
                        video = statsInboundRtp
                    } else {
                        audio = statsInboundRtp
                    }
                }
            }
            return StatisticsData(
                roundTripTime = rtt,
                audio = audio,
                video = video,
                timestamp = report.timestampUs
            )
        }

        private fun getStatisticsRoundTripTime(report: RTCStatsReport): Double? {
//            report.statsMap.
            return 0.0
        }

        private fun getStatisticsCodec(codecId: String, report: RTCStatsReport): String =
            report.statsMap.getValue(codecId).members["mimeType"] as String
    }
}

class StatsInboundRtp(
    val sid: String?,
    val kind: String,
    val decoder: String?,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val fps: Double?,
    val audioLevel: Double?,
    val totalEnergy: Double?,
    val framesReceived: Int?,
    val framesDecoded: Long?,
    val nackCount: Long?,
    val bytesReceived: BigInteger,
    val totalSampleDuration: Double?,
    val codecId: String?,
    val jitter: Double,
    val packetsReceived: Long,
    val packetsLost: Int,
    val timestamp: Long?,
    val codecName: String?
) {

    val videoResolution: String? = frameWidth?.let { frameWidth ->
        frameHeight?.let { frameHeight ->
            "$frameWidth x $frameHeight"
        }
    }
    val isVideo: Boolean = kind == "video"
}
