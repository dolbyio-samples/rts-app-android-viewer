package io.dolby.rtscomponentkit.data

import org.webrtc.RTCStatsReport
import java.math.BigInteger

data class StatisticsData(
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
                    val codecId = statsMembers["codecId"] as String?
                    val codecName = codecId?.let { getCodec(codecId, report) }
                    val statsInboundRtp = StatsInboundRtp.from(statsMembers, codecName, statsData.timestampUs)

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

        internal fun getStatisticsRoundTripTime(report: RTCStatsReport): Double? {
            report.statsMap.values.firstOrNull { it.type == "candidate-pair" && it.members["state"] == "succeeded" }
                ?.let {
                    return it.members["currentRoundTripTime"] as Double
                }
            return null
        }

        internal fun getBitrate(report: RTCStatsReport): Double? {
            report.statsMap.values.firstOrNull {
                it.type == "candidate-pair" && it.members.containsKey(
                    "availableOutgoingBitrate"
                )
            }?.let {
                return it.members["availableOutgoingBitrate"] as Double?
            }
            return null
        }

        internal fun getCodec(codecId: String, report: RTCStatsReport): String =
            report.statsMap.getValue(codecId).members["mimeType"] as String
    }
}

data class StatsInboundRtp(
    val mid: String,
    val kind: String,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val fps: Double?,
    val bytesReceived: BigInteger,
    val jitter: Double,
    val packetsLost: Int,
    val codecName: String?,
    val decoderImplementation: String?,
    val trackIdentifier: String,
    val decoder: String?,
    val processingDelay: Double?,
    val decodeTime: Double?,
    val audioLevel: Double?,
    val totalEnergy: Double?,
    val framesReceived: Int?,
    val framesDecoded: Long?,
    val framesDropped: Long?,
    val jitterBufferEmittedCount: BigInteger,
    val jitterBufferDelay: Double,
    val jitterBufferTargetDelay: Double?,
    val jitterBufferMinimumDelay: Double?,
    val nackCount: Long?,
    val totalSamplesDuration: Double?,
    val packetsReceived: Long,
    val timestamp: Double
) {
    companion object {
        fun from(
            statsMembers: MutableMap<String, Any>,
            codecName: String?,
            timestamp: Double
        ): StatsInboundRtp {
            return StatsInboundRtp(
                mid = statsMembers["mid"] as String,
                kind = statsMembers["kind"] as String,
                frameWidth = statsMembers["frameWidth"] as Long?,
                frameHeight = statsMembers["frameHeight"] as Long?,
                fps = statsMembers.getOrDefault("framesPerSecond", null) as Double?,
                bytesReceived = statsMembers["bytesReceived"] as BigInteger,
                jitter = statsMembers["jitter"] as Double,
                packetsLost = statsMembers["packetsLost"] as Int,
                codecName = codecName,
                decoderImplementation = statsMembers["decoderImplementation"] as String?,//
                trackIdentifier = statsMembers["trackIdentifier"] as String,
                decoder = statsMembers["decoder"] as String?,//
                processingDelay = statsMembers["totalProcessingDelay"] as Double?,//
                decodeTime = statsMembers["totalDecodeTime"] as Double?,//
                audioLevel = statsMembers["audioLevel"] as Double?,
                totalEnergy = statsMembers["totalAudioEnergy"] as Double?,
                framesReceived = statsMembers["framesReceived"] as Int?,//
                framesDecoded = statsMembers["framesDecoded"] as Long?,//
                framesDropped = statsMembers["framesDropped"] as Long?,//
                jitterBufferEmittedCount = statsMembers["jitterBufferEmittedCount"] as BigInteger,
                jitterBufferDelay = statsMembers["jitterBufferDelay"] as Double,
                jitterBufferTargetDelay = statsMembers["jitterBufferTargetDelay"] as Double?,
                jitterBufferMinimumDelay = statsMembers["jitterBufferMinimumDelay"] as Double?,
                nackCount = statsMembers["nackCount"] as Long?,
                totalSamplesDuration = statsMembers["totalSamplesDuration"] as Double?,
                packetsReceived = statsMembers["packetsReceived"] as Long,
                timestamp = timestamp
            )
        }
    }

    val videoResolution: String? = frameWidth?.let { frameWidth ->
        frameHeight?.let { frameHeight ->
            "$frameWidth x $frameHeight"
        }
    }
    val isVideo: Boolean = kind == "video"
}

data class MultiStreamStatisticsData(
    val roundTripTime: Double?,
    val availableOutgoingBitrate: Double?,
    val timestamp: Double?,
    val audio: List<StatsInboundRtp>?,
    val video: List<StatsInboundRtp>?
) {
    companion object {
        fun from(report: RTCStatsReport): MultiStreamStatisticsData {
            val rtt = StatisticsData.getStatisticsRoundTripTime(report)
            val bitrate = StatisticsData.getBitrate(report)
            val audio = mutableListOf<StatsInboundRtp>()
            val video = mutableListOf<StatsInboundRtp>()
            val timestamp = report.timestampUs
            val inboundRtpStreamStatsList = report.statsMap.values.filter { it.type == "inbound-rtp" }
            inboundRtpStreamStatsList.forEach { statsData ->
                val statsMembers = statsData.members
                val codecId = statsMembers["codecId"] as String?
                val codecName = codecId?.let { StatisticsData.getCodec(codecId, report) }
                val statsInboundRtp = StatsInboundRtp.from(statsMembers, codecName, statsData.timestampUs)
                if (statsInboundRtp.isVideo) {
                    video.add(statsInboundRtp)
                } else {
                    audio.add(statsInboundRtp)
                }
            }
            return MultiStreamStatisticsData(rtt, bitrate, timestamp, audio.toList(), video.toList())
        }
    }
}