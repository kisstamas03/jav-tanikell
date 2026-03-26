package com.melearning.elearning.service;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.User;
import com.melearning.elearning.repository.CourseRepository;
import com.melearning.elearning.repository.PresentationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PresentationRepository presentationRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> getPublicCourses() {
        return courseRepository.findByIsPublicTrue();
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public List<Course> getCoursesByInstructor(User instructor) {
        return courseRepository.findByInstructor(instructor);
    }

    public List<Course> getEnrolledCourses(User user) {
        return courseRepository.findByEnrolledUsersContaining(user);
    }

    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    /**
     * Kurzus törlése előtt először a prezentációkat töröljük,
     * mert azok FOREIGN KEY-el hivatkoznak a courses táblára,
     * és a cascade nem terjed ki rájuk automatikusan.
     * A quizzes és lessons CascadeType.ALL-al vannak, azok automatikusan törlődnek.
     */
    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.findById(id).ifPresent(course -> {
            // Prezentációk törlése (nincs cascade a Course->Presentation irányban)
            presentationRepository.deleteByCourse(course);
        });
        courseRepository.deleteById(id);
    }

    public void enrollUser(Course course, User user) {
        course.getEnrolledUsers().add(user);
        courseRepository.save(course);
    }

    public void unenrollUser(Course course, User user) {
        course.getEnrolledUsers().remove(user);
        courseRepository.save(course);
    }
}