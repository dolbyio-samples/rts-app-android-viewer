package io.dolby.rtscomponentkit.data

import org.webrtc.RTCStatsReport
import java.math.BigInteger

class StatisticsData(
    val roundTripTime: Double?,
    val availableOutgoingBitrate: Double?,
    val timestamp: Double?,
    val audio: StatsInboundRtp?,
    val video: StatsInboundRtp?
) {
    companion object {
        fun from(report: RTCStatsReport): StatisticsData {
            val rtt = getStatisticsRoundTripTime(report)
            val bitrate = getBitrate(report)
            var audio: StatsInboundRtp? = null
            var video: StatsInboundRtp? = null

            report.statsMap.values.forEach { statsData ->
                if (statsData.type == "inbound-rtp") {
                    val statsMembers = statsData.members
                    val codecId = statsMembers["codecId"] as String
                    val codecName = getStatisticsCodec(codecId, report)

                    val statsInboundRtp = StatsInboundRtp(
                        kind = statsMembers["kind"] as String,
                        frameWidth = statsMembers["frameWidth"] as Long?,
                        frameHeight = statsMembers["frameHeight"] as Long?,
                        fps = statsMembers.getOrDefault("framesPerSecond", null) as Double?,
                        bytesReceived = statsMembers.get("bytesReceived") as BigInteger,
                        jitter = statsMembers["jitter"] as Double,
                        packetsLost = statsMembers["packetsLost"] as Int,
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
                availableOutgoingBitrate = bitrate,
                audio = audio,
                video = video,
                timestamp = report.timestampUs
            )
        }

        private fun getStatisticsRoundTripTime(report: RTCStatsReport): Double? {
            report.statsMap.values.filter { it.type == "candidate-pair" }.forEach {
                if (it.members["state"] == "succeeded") {
                    return it.members["currentRoundTripTime"] as Double
                }
            }
            return null
        }

        private fun getBitrate(report: RTCStatsReport): Double? {
            report.statsMap.values.filter { it.type == "candidate-pair" }.forEach {
                if (it.members.containsKey("availableOutgoingBitrate")) {
                    return it.members["availableOutgoingBitrate"] as Double?
                }
            }
            return null
        }

        private fun getStatisticsCodec(codecId: String, report: RTCStatsReport): String =
            report.statsMap.getValue(codecId).members["mimeType"] as String
    }
}

class StatsInboundRtp(
    val kind: String,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val fps: Double?,
    val bytesReceived: BigInteger,
    val jitter: Double,
    val packetsLost: Int,
    val codecName: String?
) {

    val videoResolution: String? = frameWidth?.let { frameWidth ->
        frameHeight?.let { frameHeight ->
            "$frameWidth x $frameHeight"
        }
    }
    val isVideo: Boolean = kind == "video"
}
