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

    const val SRV_URI = "mongodb+srv://karankumawat640_db_user:<db_password>@studentdata.sfaysfb.mongodb.net/AttendanceData?retryWrites=true&w=majority"
    const val DEFAULT_PASSWORD = "admin"

    fun getEffectiveUri(): String {
        val pwd = if (passwordOverride.isNotBlank()) passwordOverride.trim() else DEFAULT_PASSWORD
        return SRV_URI.replace("<db_password>", pwd)
    }

    const val DATABASE_NAME = "AttendanceData"
    const val COLLECTION_STUDENTS = "students"
    const val COLLECTION_ATTENDANCE = "attendance_records"
    const val COLLECTION_ACTIVITY = "activity_logs"
}

class MongoClientManager {

    private val gson = Gson()
    private var mongoClient: MongoClient? = null
    private var lastConnectionError: String? = null

    @Synchronized
    private fun getDatabase(): MongoDatabase? {
        return try {
            if (mongoClient == null) {
                val connectionString = ConnectionString(MongoConfig.getEffectiveUri())
                val settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .applyToSocketSettings { builder ->
                        builder.connectTimeout(12, TimeUnit.SECONDS)
                        builder.readTimeout(12, TimeUnit.SECONDS)
                    }
                    .build()
                mongoClient = MongoClients.create(settings)
            }
            lastConnectionError = null
            mongoClient?.getDatabase(MongoConfig.DATABASE_NAME)
        } catch (t: Throwable) {
            t.printStackTrace()
            lastConnectionError = t.message ?: t.toString()
            null
        }
    }

    suspend fun fetchStudents(): Result<List<Student>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_STUDENTS)
            val list = mutableListOf<Student>()

            collection.find().forEach { doc ->
                val idStr = doc.getString("id") ?: doc.get("_id")?.toString() ?: "STU${System.currentTimeMillis() % 10000}"
                val name = doc.getString("name") ?: doc.getString("student_name") ?: "Unknown Student"
                val rollNumber = doc.getString("roll_number") ?: doc.getString("rollNumber") ?: doc.getString("roll_no") ?: "N/A"
                val semester = doc.getInteger("semester") ?: (doc.get("semester") as? Number)?.toInt() ?: 1
                val department = doc.getString("department") ?: "Computer Science & Engineering"
                val email = doc.getString("email") ?: ""

                list.add(
                    Student(
                        id = idStr,
                        name = name,
                        rollNumber = rollNumber,
                        semester = semester,
                        department = department,
                        email = email
                    )
                )
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    suspend fun insertStudent(student: Student): Result<Student> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_STUDENTS)
            val json = gson.toJson(student)
            val doc = Document.parse(json)
            val query = Document("id", student.id)
            collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            Result.success(student)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    suspend fun fetchAttendanceRecords(): Result<List<AttendanceRecord>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ATTENDANCE)
            val list = mutableListOf<AttendanceRecord>()

            collection.find().forEach { doc ->
                val idStr = doc.getString("id") ?: doc.get("_id")?.toString() ?: ""
                val studentId = doc.getString("student_id") ?: doc.getString("studentId") ?: ""
                val studentName = doc.getString("student_name") ?: doc.getString("studentName") ?: ""
                val semester = doc.getInteger("semester") ?: (doc.get("semester") as? Number)?.toInt() ?: 1
                val subject = doc.getString("subject") ?: ""
                val date = doc.getString("date") ?: ""
                val status = doc.getString("status") ?: "ABSENT"
                val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()

                list.add(
                    AttendanceRecord(
                        id = idStr,
                        studentId = studentId,
                        studentName = studentName,
                        semester = semester,
                        subject = subject,
                        date = date,
                        status = status,
                        timestamp = timestamp
                    )
                )
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ATTENDANCE)
            records.forEach { record ->
                val json = gson.toJson(record)
                val doc = Document.parse(json)
                val query = Document("id", record.id)
                collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            }
            Result.success(true)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    suspend fun fetchActivityLogs(): Result<List<ActivityLog>> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ACTIVITY)
            val list = mutableListOf<ActivityLog>()

            collection.find().sort(Sorts.descending("timestamp")).forEach { doc ->
                val idStr = doc.getString("id") ?: doc.get("_id")?.toString() ?: ""
                val title = doc.getString("title") ?: ""
                val description = doc.getString("description") ?: ""
                val date = doc.getString("date") ?: ""
                val semester = doc.getInteger("semester") ?: (doc.get("semester") as? Number)?.toInt() ?: 1
                val subject = doc.getString("subject") ?: ""
                val presentCount = doc.getInteger("present_count") ?: (doc.get("presentCount") as? Number)?.toInt() ?: 0
                val absentCount = doc.getInteger("absent_count") ?: (doc.get("absentCount") as? Number)?.toInt() ?: 0
                val totalCount = doc.getInteger("total_count") ?: (doc.get("totalCount") as? Number)?.toInt() ?: 0
                val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()

                list.add(
                    ActivityLog(
                        id = idStr,
                        title = title,
                        description = description,
                        date = date,
                        semester = semester,
                        subject = subject,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        totalCount = totalCount,
                        timestamp = timestamp
                    )
                )
            }
            Result.success(list)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    suspend fun insertActivityLog(log: ActivityLog): Result<ActivityLog> = withContext(Dispatchers.IO) {
        try {
            val db = getDatabase() ?: return@withContext Result.failure(Exception(lastConnectionError ?: "Could not connect to MongoDB Atlas"))
            val collection: MongoCollection<Document> = db.getCollection(MongoConfig.COLLECTION_ACTIVITY)
            val json = gson.toJson(log)
            val doc = Document.parse(json)
            val query = Document("id", log.id)
            collection.replaceOne(query, doc, ReplaceOptions().upsert(true))
            Result.success(log)
        } catch (t: Throwable) {
            Result.failure(Exception(t.message ?: t.toString()))
        }
    }

    fun resetClient() {
        try {
            mongoClient?.close()
        } catch (t: Throwable) { }
        mongoClient = null
    }
}
