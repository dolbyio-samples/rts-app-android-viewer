package io.dolby.rtscomponentkit.data

import com.squareup.moshi.Moshi
import io.dolby.rtscomponentkit.domain.StreamingConfig

class RemoteConfigService(url: String, moshi: Moshi) {
    private val client = RetrofitClient(url, moshi)

    suspend fun fetch(): StreamingConfig? {
        val response = client.fetchService.getConfig()
        return if (response.isSuccessful) response.body() else null
    }
}