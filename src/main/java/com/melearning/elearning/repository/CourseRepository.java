package com.melearning.elearning.repository;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByInstructor(User instructor);
    List<Course> findByEnrolledUsersContaining(User user);
    List<Course> findByIsPublicTrue();
}