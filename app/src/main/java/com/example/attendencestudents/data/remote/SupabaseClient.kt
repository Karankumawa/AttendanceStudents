package com.example.attendencestudents.data.remote

import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.Student
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    const val URL = "https://znncfjsmzvldlhedajbw.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_fxsPUBUp0ZomNFafRDStDA_ldJUbgPn"
    const val SECRET_KEY = ""
}

class SupabaseClient {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private fun buildRequest(endpoint: String, method: String = "GET", jsonBody: String? = null): Request {
        val fullUrl = "${SupabaseConfig.URL}/rest/v1/$endpoint"
        // Use Secret Key to ensure full admin read & write access without RLS blocking
        val apiKey = SupabaseConfig.SECRET_KEY.ifBlank { SupabaseConfig.PUBLISHABLE_KEY }

        val requestBuilder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")

        when (method) {
            "POST" -> {
                requestBuilder.addHeader("Prefer", "return=representation,resolution=merge-duplicates")
                val body = jsonBody?.toRequestBody(jsonMediaType) ?: "".toRequestBody(jsonMediaType)
                requestBuilder.post(body)
            }
            "DELETE" -> {
                requestBuilder.delete()
            }
            else -> {
                requestBuilder.addHeader("Prefer", "return=representation")
                requestBuilder.get()
            }
        }
        return requestBuilder.build()
    }

    suspend fun fetchStudents(): Result<List<Student>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("students?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<Student>>() {}.type
                    val list: List<Student> = gson.fromJson(bodyString, type) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertStudent(student: Student): Result<Student> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(student)
            val request = buildRequest("students", method = "POST", jsonBody = json)
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(student)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAttendanceRecords(): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("attendance_records?select=*")
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<AttendanceRecord>>() {}.type
                    val list: List<AttendanceRecord> = gson.fromJson(bodyString, type) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(records)
            val request = buildRequest("attendance_records", method = "POST", jsonBody = json)
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchActivityLogs(): Result<List<ActivityLog>> = withContext(Dispatchers.IO) {
        try {
            val request = buildRequest("activity_logs?select=*&order=timestamp.desc")
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val type = object : TypeToken<List<ActivityLog>>() {}.type
                    val list: List<ActivityLog> = gson.fromJson(bodyString, type) ?: emptyList()
                    Result.success(list)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertActivityLog(log: ActivityLog): Result<ActivityLog> = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(log)
            val request = buildRequest("activity_logs", method = "POST", jsonBody = json)
            okHttpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success(log)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
