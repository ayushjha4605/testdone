package com.testdone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.testdone.app.data.local.entity.ContentVersionEntity
import com.testdone.app.data.local.entity.QuestionEntity
import com.testdone.app.data.local.entity.SubjectEntity
import com.testdone.app.data.local.entity.TestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    // ── subjects ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Query("SELECT * FROM subjects WHERE examId = :examId")
    suspend fun subjectsForExam(examId: String): List<SubjectEntity>

    // ── questions ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun questionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE examId = :examId AND subjectId = :subjectId ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun questionsForSubject(examId: String, subjectId: String, limit: Int, offset: Int): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE examId = :examId AND subjectId = :subjectId")
    suspend fun countForSubject(examId: String, subjectId: String): Int

    @Query("SELECT COUNT(*) FROM questions WHERE examId = :examId AND subjectId = :subjectId AND topic = :topic")
    suspend fun countForTopic(examId: String, subjectId: String, topic: String): Int

    @Query("SELECT * FROM questions WHERE examId = :examId AND subjectId = :subjectId AND topic = :topic ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun questionsForTopic(examId: String, subjectId: String, topic: String, limit: Int, offset: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY id LIMIT :limit OFFSET :offset")
    suspend fun questionsForExam(examId: String, limit: Int, offset: Int): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions WHERE examId = :examId")
    suspend fun countForExam(examId: String): Int

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun totalQuestions(): Int

    // ── tests ───────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTests(tests: List<TestEntity>)

    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun testById(testId: String): TestEntity?

    @Query("SELECT * FROM tests WHERE examId = :examId AND kind = :kind ORDER BY id")
    suspend fun testsForExam(examId: String, kind: String): List<TestEntity>

    @Query("SELECT * FROM tests WHERE examId = :examId ORDER BY id")
    suspend fun allTestsForExam(examId: String): List<TestEntity>

    @Query("SELECT COUNT(*) FROM tests")
    suspend fun totalTests(): Int

    // ── versions ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVersion(v: ContentVersionEntity)

    @Query("SELECT * FROM content_versions")
    suspend fun allVersions(): List<ContentVersionEntity>

    @Query("SELECT * FROM content_versions WHERE examId = :examId")
    suspend fun versionFor(examId: String): ContentVersionEntity?

    @Query("SELECT COUNT(*) FROM content_versions")
    suspend fun versionCount(): Int

    // ── full exam replacement (OTA pack install) ────────────────────────────
    @Query("DELETE FROM questions WHERE examId = :examId")
    suspend fun deleteQuestionsFor(examId: String)

    @Query("DELETE FROM tests WHERE examId = :examId")
    suspend fun deleteTestsFor(examId: String)

    @Query("DELETE FROM subjects WHERE examId = :examId")
    suspend fun deleteSubjectsFor(examId: String)

    @Transaction
    suspend fun replaceExamContent(
        examId: String,
        subjects: List<SubjectEntity>,
        questions: List<QuestionEntity>,
        tests: List<TestEntity>,
        version: ContentVersionEntity,
    ) {
        deleteSubjectsFor(examId)
        deleteQuestionsFor(examId)
        deleteTestsFor(examId)
        insertSubjects(subjects)
        insertQuestions(questions)
        insertTests(tests)
        upsertVersion(version)
    }

    // ── seeding progress (Flow) ─────────────────────────────────────────────
    @Query("SELECT COUNT(DISTINCT examId) FROM subjects")
    fun seededExamCountFlow(): Flow<Int>
}
