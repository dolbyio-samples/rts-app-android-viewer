package io.dolby.rtscomponentkit.data

import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class RetrofitClient(url: String, moshi: Moshi) {
    private val retrofit = Retrofit.Builder()
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val fetchService: APIService = retrofit.create(APIService::class.java)
}