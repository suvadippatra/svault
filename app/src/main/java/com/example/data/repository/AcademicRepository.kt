package com.example.data.repository

import com.example.data.dao.AcademicItemDao
import com.example.data.model.AcademicItem
import com.example.data.model.CourseWithSemesters
import com.example.data.model.Semester
import kotlinx.coroutines.flow.Flow

class AcademicRepository(
    private val academicItemDao: AcademicItemDao
) {
    fun getAllCoursesWithSemesters(): Flow<List<CourseWithSemesters>> {
        return academicItemDao.getAllCoursesWithSemesters()
    }

    fun getAllAcademicItems(): Flow<List<AcademicItem>> {
        return academicItemDao.getAllAcademicItems()
    }

    fun getCourseWithSemestersById(id: String): Flow<CourseWithSemesters?> {
        return academicItemDao.getCourseWithSemestersById(id)
    }

    fun getLinksForAcademicItem(id: String): Flow<List<com.example.data.model.AcademicDocumentLink>> {
        return academicItemDao.getLinksForAcademicItem(id)
    }

    suspend fun insert(academicItem: AcademicItem) {
        academicItemDao.insert(academicItem)
    }

    suspend fun insertSemester(semester: Semester) {
        academicItemDao.insertSemester(semester)
    }

    suspend fun insertAcademicDocumentLink(link: com.example.data.model.AcademicDocumentLink) {
        academicItemDao.insertAcademicDocumentLink(link)
    }

    suspend fun deleteSemestersForCourse(courseId: String) {
        academicItemDao.deleteSemestersForCourse(courseId)
    }
}
