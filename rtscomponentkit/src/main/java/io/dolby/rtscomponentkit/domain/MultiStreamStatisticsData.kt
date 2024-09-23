package io.dolby.rtscomponentkit.domain

import android.icu.text.SimpleDateFormat
import android.util.Log
import com.millicast.clients.stats.Codecs
import com.millicast.clients.stats.InboundRtpStream
import com.millicast.clients.stats.RemoteInboundRtpStream
import com.millicast.clients.stats.RemoteOutboundRtpStream
import com.millicast.clients.stats.RtsReport
import com.millicast.clients.stats.StatsType
import io.dolby.rtscomponentkit.R
import org.webrtc.RTCStatsReport
import java.math.BigInteger
import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Date

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
                    val statsInboundRtp =
                        StatsInboundRtp.from(statsMembers, codecName, statsData.timestampUs)

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
            report.statsMap.values.firstOrNull { it.type == "candidate-pair" && it.members["state"] == "succeeded" }
                ?.let {
                    return it.members["currentRoundTripTime"] as Double
                }
            return null
        }

        internal fun getStatisticsRoundTripTime(report: RtsReport): Double? {
            val remoteStream = report.stats()
                .find { it is RemoteOutboundRtpStream && it.statsType() == StatsType.REMOTE_OUTBOUND_RTP }
            remoteStream?.let {
                return (it as RemoteOutboundRtpStream).roundTripTime
            }
            return null
        }

        internal fun getBitrate(report: RtsReport): Double? {
//            val remoteStream = report.stats()
//                .find { it is RemoteOutboundRtpStream && it.statsType() == StatsType.REMOTE_OUTBOUND_RTP }
//            remoteStream?.let {
//                return (it as RemoteOutboundRtpStream).totalRoundTripTime
//            }
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

        private fun getCodec(codecId: String, report: RTCStatsReport): String =
            report.statsMap.getValue(codecId).members["mimeType"] as String

        internal fun getCodec(codecId: String, report: RtsReport): String? {
            val codec: Codecs? = report.stats()
                .firstOrNull { it is Codecs && it.id == codecId && it.statsType() == StatsType.CODEC } as Codecs?
            return codec?.mimeType
        }
    }
}

data class StatsInboundRtp(
    val mid: String,
    val kind: String,
    val frameWidth: Long?,
    val frameHeight: Long?,
    val fps: Double?,
    val bytesReceived: BigInteger?,
    val jitter: Double?,
    val packetsLost: Long?,
    val codecName: String?,
    val decoderImplementation: String?,
    val trackIdentifier: String?,
    val decoder: String?,
    val processingDelay: Double?,
    val decodeTime: Double?,
    val audioLevel: Double?,
    val totalEnergy: Double?,
    val framesReceived: Long?,
    val framesDecoded: Long?,
    val framesDropped: Long?,
    val jitterBufferEmittedCount: BigInteger,
    val jitterBufferDelay: Double?,
    val jitterBufferTargetDelay: Double?,
    val jitterBufferMinimumDelay: Double?,
    val nackCount: Long?,
    val totalSamplesDuration: Double?,
    val packetsReceived: Long?,
    val timestamp: Long
) {
    val videoResolution: String? = frameWidth?.let { frameWidth ->
        frameHeight?.let { frameHeight ->
            "$frameWidth x $frameHeight"
        }
    }
    val isVideo: Boolean = kind == "video"

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
                packetsLost = (statsMembers["packetsLost"] as UInt?)?.toLong(),
                codecName = codecName,
                decoderImplementation = statsMembers["decoderImplementation"] as String?, //
                trackIdentifier = statsMembers["trackIdentifier"] as String,
                decoder = statsMembers["decoder"] as String?, //
                processingDelay = statsMembers["totalProcessingDelay"] as Double?, //
                decodeTime = statsMembers["totalDecodeTime"] as Double?, //
                audioLevel = statsMembers["audioLevel"] as Double?,
                totalEnergy = statsMembers["totalAudioEnergy"] as Double?,
                framesReceived = statsMembers["framesReceived"] as Long?, //
                framesDecoded = statsMembers["framesDecoded"] as Long?, //
                framesDropped = statsMembers["framesDropped"] as Long?, //
                jitterBufferEmittedCount = statsMembers["jitterBufferEmittedCount"] as BigInteger,
                jitterBufferDelay = statsMembers["jitterBufferDelay"] as Double,
                jitterBufferTargetDelay = statsMembers["jitterBufferTargetDelay"] as Double?,
                jitterBufferMinimumDelay = statsMembers["jitterBufferMinimumDelay"] as Double?,
                nackCount = statsMembers["nackCount"] as Long?,
                totalSamplesDuration = statsMembers["totalSamplesDuration"] as Double?,
                packetsReceived = statsMembers["packetsReceived"] as Long,
                timestamp = timestamp.toLong()
            )
        }

        fun from(
            statsMembers: InboundRtpStream,
            codecName: String?
        ): StatsInboundRtp {
            return StatsInboundRtp(
                mid = statsMembers.mid ?: "null",
                kind = statsMembers.kind,
                frameWidth = statsMembers.frameWidth?.toLong(),
                frameHeight = statsMembers.frameHeight?.toLong(),
                fps = statsMembers.framesPerSecond,
                bytesReceived = BigInteger.valueOf(statsMembers.bytesReceived?.toLong() ?: 0),
                jitter = statsMembers.jitter,
                packetsLost = statsMembers.packetsLost?.toLong(),
                codecName = codecName,
                decoderImplementation = statsMembers.decoderImplementation,
                trackIdentifier = statsMembers.trackIdentifier,
                decoder = statsMembers.decoderImplementation,
                processingDelay = statsMembers.totalProcessingDelay,
                decodeTime = statsMembers.totalDecodeTime,
                audioLevel = statsMembers.audioLevel,
                totalEnergy = statsMembers.totalAudioEnergy,
                framesReceived = statsMembers.framesReceived?.toLong(),
                framesDecoded = statsMembers.framesDecoded?.toLong(),
                framesDropped = statsMembers.framesDropped?.toLong(),
                jitterBufferEmittedCount = BigInteger.valueOf(statsMembers.jitterBufferEmittedCount?.toLong() ?: 0),
                jitterBufferDelay = statsMembers.jitterBufferDelay,
                jitterBufferTargetDelay = statsMembers.jitterBufferTargetDelay,
                jitterBufferMinimumDelay = statsMembers.jitterBufferMinimumDelay,
                nackCount = statsMembers.nackCount?.toLong(),
                totalSamplesDuration = statsMembers.totalSamplesDuration,
                packetsReceived = statsMembers.packetsReceived?.toLong(),
                timestamp = statsMembers.timestamp.toLong()
            )
        }

        fun getStatisticsValuesList(statisticsData: StatisticsData?): List<Pair<Int, String>>? {
            statisticsData?.let { statistics ->
                val statisticsValuesList = inboundRtpAudioVideoDataToList(
                    audioStatistics = statistics.audio,
                    videoStatistics = statistics.video
                ).toMutableList()
                statistics.roundTripTime?.let {
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_rtt,
                            "${it.times(1000).toLong()} ms"
                        )
                    )
                }

                statistics.timestamp?.let {
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_timestamp,
                            String.format("%.0f", it)
                        )
                    )
                }

                statistics.video?.codecName?.let {
                    statisticsValuesList.add(Pair(R.string.statisticsScreen_codecs, it))
                }

                return statisticsValuesList
            }
            return null
        }

        fun inboundRtpAudioVideoDataToList(
            videoStatistics: StatsInboundRtp?,
            audioStatistics: StatsInboundRtp?
        ): List<Pair<Int, String>> {
            val statisticsValuesList = mutableListOf<Pair<Int, String>>()
            if (audioStatistics?.mid != null || videoStatistics?.mid != null) {
                val midValues = videoStatistics?.mid?.let { it + ", " + audioStatistics?.mid }
                    ?: audioStatistics?.mid ?: ""
                statisticsValuesList.add(Pair(R.string.statisticsScreen_mid, midValues))
            }

            videoStatistics?.decoderImplementation?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_decoder_impl, it))
            }

            videoStatistics?.processingDelay?.let { pd ->
                videoStatistics.framesDecoded?.let { fd ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_processing_delay,
                            String.format("%.2f ms", msNormalised(pd, fd.toDouble()))
                        )
                    )
                }
            }

            videoStatistics?.decodeTime?.let { dt ->
                videoStatistics.framesDecoded?.let { fd ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_decode_time,
                            String.format("%.2f ms", msNormalised(dt, fd.toDouble()))
                        )
                    )
                }
            }

            videoStatistics?.videoResolution?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoResolution, it))
            }

            videoStatistics?.fps?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_fps, "${it.toLong()}"))
            }

            audioStatistics?.bytesReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audioTotal,
                        formattedByteCount(it.toLong())
                    )
                )
            }

            videoStatistics?.bytesReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoTotal,
                        formattedByteCount(it.toLong())
                    )
                )
            }

            audioStatistics?.packetsReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audio_packets_received,
                        "$it"
                    )
                )
            }

            videoStatistics?.packetsReceived?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_video_packets_received,
                        "$it"
                    )
                )
            }

            audioStatistics?.framesDecoded?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_frames_decoded, "$it"))
            }

            videoStatistics?.framesDecoded?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_frames_decoded, "$it"))
            }

            audioStatistics?.framesDropped?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_audio_packets_dropped,
                        "$it"
                    )
                )
            }

            videoStatistics?.framesDropped?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_video_packets_dropped,
                        "$it"
                    )
                )
            }

            videoStatistics?.jitterBufferEmittedCount?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_jitter_bufffer_ec,
                        "$it"
                    )
                )
            }

            videoStatistics?.jitter?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_videoJitter,
                        "${it.times(1000)} ms"
                    )
                )
            }

            audioStatistics?.jitterBufferDelay?.let { jbd ->
                audioStatistics.jitterBufferEmittedCount.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_audio_jitter_bufffer_delay,
                            String.format("%.2f ms", msNormalised(jbd, jbec.toDouble()))
                        )
                    )
                }
            }

            videoStatistics?.jitterBufferDelay?.let { jbd ->
                videoStatistics.jitterBufferEmittedCount.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_video_jitter_bufffer_target_delay,
                            String.format("%.2f ms", msNormalised(jbd, jbec.toDouble()))
                        )
                    )
                }
            }

            videoStatistics?.jitterBufferMinimumDelay?.let { jbmd ->
                videoStatistics.jitterBufferEmittedCount.let { jbec ->
                    statisticsValuesList.add(
                        Pair(
                            R.string.statisticsScreen_jitter_bufffer_min_delay,
                            String.format("%.2f ms", msNormalised(jbmd, jbec.toDouble()))
                        )
                    )
                }
            }

            audioStatistics?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_audioLoss, "$it"))
            }

            videoStatistics?.packetsLost?.let {
                statisticsValuesList.add(Pair(R.string.statisticsScreen_videoLoss, "$it"))
            }

            audioStatistics?.codecName?.let { audioCodec ->
                val codecs = audioCodec + (
                    videoStatistics?.codecName?.let { videoCodec ->
                        ", $videoCodec"
                    } ?: ""
                    )
                statisticsValuesList.add(Pair(R.string.statisticsScreen_codecs, codecs))
            } ?: {
                videoStatistics?.codecName?.let { videoCodec ->
                    statisticsValuesList.add(
                        Pair(R.string.statisticsScreen_codecs, videoCodec)
                    )
                }
            }

            videoStatistics?.timestamp?.let {
                statisticsValuesList.add(
                    Pair(
                        R.string.statisticsScreen_timestamp,
                        "${getDateTime(it)}"
                    )
                )
            }

            return statisticsValuesList
        }

        private fun getDateTime(timeStamp: Long): String? {
            return try {
                val dateFormat = SimpleDateFormat.getDateTimeInstance()
                val netDate = Date(timeStamp)
                dateFormat.format(netDate)
            } catch (e: Exception) {
                Log.e("StatisticsData", e.toString())
                null
            }
        }

        private fun formattedByteCount(bytes: Long): String {
            var value = bytes
            if (-1000 < value && value < 1000) {
                return "$value B"
            }
            val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
            while (value <= -999950 || value >= 999950) {
                value /= 1000
                ci.next()
            }
            return String.format("%.1f %cB", value / 1000.0, ci.current())
        }

        fun msNormalised(numerator: Double, denominator: Double): Double {
            return if (denominator == 0.0) {
                0.0
            } else {
                numerator * 1000 / denominator
            }
        }
    }
}

data class MultiStreamStatisticsData(
    val roundTripTime: Double?,
    val availableOutgoingBitrate: Double?,
    val timestamp: Double?,
    val audio: List<StatsInboundRtp>?,
    val video: List<StatsInboundRtp>?
) {
    companion object {
        fun from(report: RtsReport): MultiStreamStatisticsData {
            val rtt = StatisticsData.getStatisticsRoundTripTime(report)
            val bitrate = StatisticsData.getBitrate(report)
            val audio = mutableListOf<StatsInboundRtp>()
            val video = mutableListOf<StatsInboundRtp>()
            val inboundRtpStreams = report.stats()
                .filter { it is InboundRtpStream && it.statsType() == StatsType.INBOUND_RTP }
            val timestamp = inboundRtpStreams.firstOrNull()?.timestamp?.toDouble()
            inboundRtpStreams.forEach { statsData ->
                val inboundData = statsData as InboundRtpStream
                val codecId = inboundData.codecId
                val codecName = codecId?.let { StatisticsData.getCodec(codecId, report) }
                val statsInboundRtp =
                    StatsInboundRtp.from(inboundData, codecName)
                if (statsInboundRtp.isVideo) {
                    video.add(statsInboundRtp)
                } else {
                    audio.add(statsInboundRtp)
                }
            }
            var roundTripTime = 0.0
            val lastRemoteInBoundRTPStream = report.stats()
                .filter { it is RemoteInboundRtpStream && it.statsType() == StatsType.REMOTE_INBOUND_RTP }
                .lastOrNull()
            lastRemoteInBoundRTPStream?.let {
                roundTripTime = (it as RemoteInboundRtpStream).roundTripTime
            }
            return MultiStreamStatisticsData(
                roundTripTime,
                bitrate,
                timestamp,
                audio.toList(),
                video.toList()
            )
        }
    }
}
