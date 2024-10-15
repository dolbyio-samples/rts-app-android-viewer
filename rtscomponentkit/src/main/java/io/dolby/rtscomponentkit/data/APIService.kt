package io.dolby.rtscomponentkit.data

import io.dolby.rtscomponentkit.domain.RemoteStreamConfig
import retrofit2.Response
import retrofit2.http.GET

interface APIService {
    @GET()
    suspend fun getConfig() : Response<RemoteStreamConfig>
}