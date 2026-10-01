package com.example.attendencestudents.data.remote

import com.example.attendencestudents.data.model.ActivityLog
import com.example.attendencestudents.data.model.AttendanceRecord
import com.example.attendencestudents.data.model.Student
import com.google.gson.Gson
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Sorts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.concurrent.TimeUnit

object MongoConfig {
    var passwordOverride: String = ""
    const val RAW_URI = "mongodb+srv://karankumawat640_db_user:<db_password>@studentdata.sfaysfb.mongodb.net/?appName=StudentData&compressors=zlib"
    const val DEFAULT_PASSWORD = "admin"

    fun getEffectiveUri(): String {
        val pwd = if (passwordOverride.isNotBlank()) passwordOverride else DEFAULT_PASSWORD
        return RAW_URI.replace("<db_password>", pwd)
    }

    const val DATABASE_NAME = "StudentAttendanceDB"
    const val COLLECTION_STUDENTS = "students"
    const val COLLECTION_ATTENDANCE = "attendance_records"
    const val COLLECTION_ACTIVITY = "activity_logs"
}

class MongoClientManager {

    private val gson = Gson()
    private var mongoClient: MongoClient? = null

    @Synchronized
    private fun getDatabase(): MongoDatabase? {
        return try {
            if (mongoClient == null) {
                val connectionString = ConnectionString(MongoConfig.getEffectiveUri())
                val settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSocketSettings { builder ->
                        builder.connectTimeout(10, TimeUnit.SECONDS)
                        builder.readTimeout(10, TimeUnit.SECONDS)
                    }
                    .build()
                mongoClient = MongoClients.create(settings)
            }
            mongoClient?.getDatabase(MongoConfig.DATABASE_NAME)
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    suspend fun fetchStudents(): Result<List<Student>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_STUDENTS)
            val list = mutableListOf<Student>()
            collection.find().forEach { doc ->
                val json = doc.toJson()
                val student = gson.fromJson(json, Student::class.java)
                if (student != null) list.add(student)
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Error"))
        }
    }

    suspend fun insertStudent(student: Student): Result<Student> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_STUDENTS)
            val json = gson.toJson(student)
            val doc = Document.parse(json)
            val query = Document("id", student.id)
            collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            Result.success(student)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Insert Error"))
        }
    }

    suspend fun fetchAttendanceRecords(): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ATTENDANCE)
            val list = mutableListOf<AttendanceRecord>()
            collection.find().forEach { doc ->
                val json = doc.toJson()
                val record = gson.fromJson(json, AttendanceRecord::class.java)
                if (record != null) list.add(record)
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Error"))
        }
    }

    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ATTENDANCE)
            records.forEach { record ->
                val json = gson.toJson(record)
                val doc = Document.parse(json)
                val query = Document("id", record.id)
                collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            }
            Result.success(true)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Insert Error"))
        }
    }

    suspend fun fetchActivityLogs(): Result<List<ActivityLog>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ACTIVITY)
            val list = mutableListOf<ActivityLog>()
            collection.find().sort(Sorts.descending("timestamp")).forEach { doc ->
                val json = doc.toJson()
                val log = gson.fromJson(json, ActivityLog::class.java)
                if (log != null) list.add(log)
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Error"))
        }
    }

    suspend fun insertActivityLog(log: ActivityLog): Result<ActivityLog> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception("Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ACTIVITY)
            val json = gson.toJson(log)
            val doc = Document.parse(json)
            val query = Document("id", log.id)
            collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            Result.success(log)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: "MongoDB Insert Error"))
        }
    }

    fun resetClient() {
        try {
            mongoClient?.close()
        } catch (t: Throwable) { }
        mongoClient = null
    }
}
