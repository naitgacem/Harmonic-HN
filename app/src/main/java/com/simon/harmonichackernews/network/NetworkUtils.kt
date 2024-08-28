package com.simon.harmonichackernews.network

import com.simon.harmonichackernews.data.NetworkResult
import retrofit2.HttpException
import retrofit2.Response

suspend fun <T : Any> handleApi(
    execute: suspend () -> Response<T>
): NetworkResult<T> {
    return try {
        val response = execute()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            NetworkResult.Success(body)
        } else {
            NetworkResult.Error(code = response.code())
        }
    } catch (e: HttpException) {
        e.printStackTrace()
        NetworkResult.Error(code = e.code())
    } catch (e: Throwable) {
        e.printStackTrace()
        NetworkResult.Exception(e)
    }
}