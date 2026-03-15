package com.melearning.elearning.repository;

import com.melearning.elearning.model.Quiz;
import com.melearning.elearning.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByCourse(Course course);
}