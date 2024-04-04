package io.dolby.rtscomponentkit.data

import com.millicast.clients.stats.Codecs
import com.millicast.clients.stats.InboundRtpStream
import com.millicast.clients.stats.RemoteOutboundRtpStream
import com.millicast.clients.stats.RtsReport
import com.millicast.clients.stats.StatsType
import org.webrtc.RTCStatsReport
import java.math.BigInteger

data class SingleStreamStatisticsData(
    val roundTripTime: Double?,
    val availableOutgoingBitrate: Double?,
    val timestamp: Long?,
    val audio: StatsInboundRtp?,
    val video: StatsInboundRtp?
) {
    companion object {
        fun from(report: RTCStatsReport): SingleStreamStatisticsData {
            val rtt = getStatisticsRoundTripTime(report)
            val bitrate = getBitrate(report)
            var audio: StatsInboundRtp? = null
            var video: StatsInboundRtp? = null

            report.statsMap.values.forEach { statsData ->
                if (statsData.type == "inbound-rtp") {
                    val statsMembers = statsData.members
                    val codecId = statsMembers["codecId"] as String?
                    val codecName = codecId?.let { getCodec(codecId, report) }

                    val statsInboundRtp = StatsInboundRtp(
                        kind = statsMembers["kind"] as String,
                        frameWidth = statsMembers["frameWidth"] as Long?,
                        frameHeight = statsMembers["frameHeight"] as Long?,
                        fps = statsMembers.getOrDefault("framesPerSecond", null) as Double?,
                        bytesReceived = statsMembers.get("bytesReceived") as BigInteger,
                        jitter = statsMembers["jitter"] as Double?,
                        packetsLost = statsMembers["packetsLost"] as Long?,
                        codecName = codecName
                    )
                    if (statsInboundRtp.isVideo) {
                        video = statsInboundRtp
                    } else {
                        audio = statsInboundRtp
                    }
                }
            }
            return SingleStreamStatisticsData(
                roundTripTime = rtt,
                availableOutgoingBitrate = bitrate,
                audio = audio,
                video = video,
                timestamp = report.timestampUs.toLong()
            )
        }

        fun from(report: RtsReport): SingleStreamStatisticsData {
            val rtt = getStatisticsRoundTripTime(report)
            val bitrate = getBitrate(report)
            var audio: StatsInboundRtp? = null
            var video: StatsInboundRtp? = null
            val inboundRtpStreams = report.stats()
                .filter { it is InboundRtpStream && it.statsType() == StatsType.INBOUND_RTP }
            val timestamp = inboundRtpStreams.firstOrNull()?.timestamp?.toLong()
            inboundRtpStreams.forEach { statsData ->
                val inboundData = statsData as InboundRtpStream
                val codecId = inboundData.codecId
                val codecName = codecId?.let { getCodec(codecId, report) }
                val statsInboundRtp = StatsInboundRtp(
                    kind = inboundData.kind,
                    frameWidth = inboundData.frameWidth?.toLong(),
                    frameHeight = inboundData.frameHeight?.toLong(),
                    fps = inboundData.framesPerSecond,
                    bytesReceived = BigInteger.valueOf(inboundData.bytesReceived?.toLong() ?: 0),
                    jitter = inboundData.jitter,
                    packetsLost = inboundData.packetsLost,
                    codecName = codecName
                )
                if (statsInboundRtp.isVideo) {
                    video = statsInboundRtp
                } else {
                    audio = statsInboundRtp
                }
            }
            return SingleStreamStatisticsData(
                roundTripTime = rtt,
                availableOutgoingBitrate = bitrate,
                timestamp = timestamp,
                audio = audio,
                video = video
            )
        }

        private fun getStatisticsRoundTripTime(report: RTCStatsReport): Double? {
            report.statsMap.values.firstOrNull { it.type == "candidate-pair" && it.members["state"] == "succeeded" }
                ?.let {
                    return it.members["currentRoundTripTime"] as Double
                }
            return null
        }

        private fun getBitrate(report: RTCStatsReport): Double? {
            report.statsMap.values.firstOrNull {
                it.type == "candidate-pair" && it.members.containsKey(
                    "availableOutgoingBitrate"
                )
            }?.let {
                return it.members["availableOutgoingBitrate"] as Double?
            }
            return null
        }
        private fun getStatisticsRoundTripTime(report: RtsReport): Double? {
            val remoteStream = report.stats()
                .find { it is RemoteOutboundRtpStream && it.statsType() == StatsType.REMOTE_OUTBOUND_RTP }
            remoteStream?.let {
                return (it as RemoteOutboundRtpStream).roundTripTime
            }
            return null
        }

        private fun getBitrate(report: RtsReport): Double? {
//            val remoteStream = report.stats()
//                .find { it is RemoteOutboundRtpStream && it.statsType() == StatsType.REMOTE_OUTBOUND_RTP }
//            remoteStream?.let {
//                return (it as RemoteOutboundRtpStream).availableOutgoingBitrate
//            }
            return null
        }

        private fun getCodec(codecId: String, report: RTCStatsReport): String =
            report.statsMap.getValue(codecId).members["mimeType"] as String

        private fun getCodec(codecId: String, report: RtsReport): String? {
            val codec: Codecs? = report.stats()
                .firstOrNull { it is Codecs && it.id == codecId && it.statsType() == StatsType.CODEC } as Codecs?
            return codec?.mimeType
        }
    }

    override fun toString(): String {
        return "rtt: $roundTripTime, availableOutgoigBitrate: $availableOutgoingBitrate, video: $video"
    }
}

data class StatsInboundRtp(
    val kind: String,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val fps: Double?,
    val bytesReceived: BigInteger,
    val jitter: Double?,
    val packetsLost: Long?,
    val codecName: String?
) {

    val videoResolution: String? = frameWidth?.let { frameWidth ->
        frameHeight?.let { frameHeight ->
            "$frameWidth x $frameHeight"
        }
    }
    val isVideo: Boolean = kind == "video"

    override fun toString(): String {
        return "frameWidth: $frameWidth, frameHeight: $frameHeight, fps: $fps, bytesReceived: $bytesReceived, $jitter, $packetsLost, $codecName"
    }
}
