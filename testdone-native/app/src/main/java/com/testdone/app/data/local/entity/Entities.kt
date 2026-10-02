package com.testdone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "questions", indices = [Index("examId"), Index(value = ["examId", "subjectId"]), Index(value = ["examId", "topic"])])
data class QuestionEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val subjectId: String,
    val subjectName: String,
    val topic: String,
    val text: String,
    val optionsJson: String,
    val correct: Int,
    val solution: String,
    val difficulty: Int, // 0 Easy, 1 Moderate, 2 Hard
    val marks: Double,
    val isPyq: Boolean,
    val year: Int?,
)

@Entity(tableName = "tests", indices = [Index("examId"), Index(value = ["examId", "kind"])])
data class TestEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val kind: String, // mock | pyq
    val title: String,
    val desc: String,
    val durationMin: Int,
    val totalQuestions: Int,
    val maxMarks: Double,
    val neg: Double,
    val attempt: Int?,
    val year: Int?,
    val questionIdsJson: String,
)

@Entity(tableName = "subjects", primaryKeys = ["examId", "id"])
data class SubjectEntity(
    val examId: String,
    val id: String,
    val name: String,
    val topicsJson: String,
)

@Entity(tableName = "content_versions")
data class ContentVersionEntity(
    @PrimaryKey val examId: String,
    val version: Int,
    val questionCount: Int,
    val mockCount: Int,
    val pyqCount: Int,
)

@Entity(tableName = "attempts", indices = [Index("testId"), Index("examId"), Index(value = ["startedAt"]), Index("synced")])
data class AttemptEntity(
    @PrimaryKey val attemptId: String,
    val testId: String,
    val testTitle: String,
    val testKind: String,
    val examId: String,
    val startedAt: Long,
    val completedAt: Long?,
    val durationSec: Int,
    val totalQuestions: Int,
    val attempted: Int,
    val correct: Int,
    val incorrect: Int,
    val score: Double,
    val maxScore: Double,
    val accuracy: Double,
    val percentile: Double?,
    val rank: Long?,
    val sectionResultsJson: String,
    val questionAttemptsJson: String,
    val synced: Boolean = false,
)
