package io.dolby.rtscomponentkit.data

import android.net.Uri
import com.squareup.moshi.Moshi
import io.dolby.rtscomponentkit.domain.RemoteStreamConfig

class RemoteConfigService(url: String, moshi: Moshi) {
    private val endPoint: String = Uri.parse(url).lastPathSegment ?: ""
    private val baseUrl: String = url.removeSuffix(endPoint)
    private val client = RetrofitClient(baseUrl = baseUrl, moshi = moshi)

    suspend fun fetch(): RemoteStreamConfig? {
        val response = client.fetchService.getConfig(endPoint)
        return if (response.isSuccessful) response.body() else null
    }
}