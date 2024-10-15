package io.dolby.rtscomponentkit.data

import io.dolby.rtscomponentkit.domain.RemoteStreamConfig
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface APIService {
    @GET("{endPoint}")
    suspend fun getConfig(@Path("endPoint") endPoint: String): Response<RemoteStreamConfig>
}