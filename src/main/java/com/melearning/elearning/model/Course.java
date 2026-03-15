package com.melearning.elearning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    private Boolean isPublic = true;

    @ManyToOne
    @JoinColumn(name = "instructor_id")
    private User instructor;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lesson> lessons = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quiz> quizzes = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "course_enrollments",
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> enrolledUsers = new HashSet<>();

    public Course() {
        this.createdAt = LocalDateTime.now();
    }

    public Course(String title, String description, User instructor) {
        this();
        this.title = title;
        this.description = description;
        this.instructor = instructor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Lesson> getLessons() { return lessons; }
    public void setLessons(List<Lesson> lessons) { this.lessons = lessons; }

    public Set<User> getEnrolledUsers() { return enrolledUsers; }
    public void setEnrolledUsers(Set<User> enrolledUsers) { this.enrolledUsers = enrolledUsers; }

    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }

    public List<Quiz> getQuizzes() { return quizzes; }
    public void setQuizzes(List<Quiz> quizzes) { this.quizzes = quizzes; }
}