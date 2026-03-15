package com.melearning.elearning.repository;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByCourseOrderByOrderIndex(Course course);
}