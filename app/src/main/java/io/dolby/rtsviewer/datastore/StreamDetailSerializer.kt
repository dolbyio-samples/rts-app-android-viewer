package io.dolby.rtsviewer.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class RecentStreamsSerializer : Serializer<RecentStreams> {
    override val defaultValue: RecentStreams = RecentStreams.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): RecentStreams = withContext(Dispatchers.IO) {
        try {
            return@withContext RecentStreams.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto")
        }
    }

    override suspend fun writeTo(t: RecentStreams, output: OutputStream) = withContext(Dispatchers.IO) {
        try {
            t.writeTo(output)
        } catch (e: java.io.IOException) {
            throw CorruptionException("Cannot write proto $e")
        }
    }
}

val Context.recentStreamsDataStore: DataStore<RecentStreams> by dataStore(
    fileName = "recent_streams.pb",
    serializer = RecentStreamsSerializer()
)
