package com.melearning.elearning.repository;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PresentationRepository extends JpaRepository<Presentation, Long> {
    List<Presentation> findByCourseOrderByOrderIndex(Course course);
}