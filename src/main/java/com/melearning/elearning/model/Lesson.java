package com.melearning.elearning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer orderIndex;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDateTime createdAt;

    // Konstruktorok
    public Lesson() {
        this.createdAt = LocalDateTime.now();
    }

    public Lesson(String title, String content, Integer orderIndex, Course course) {
        this();
        this.title = title;
        this.content = content;
        this.orderIndex = orderIndex;
        this.course = course;
    }

    // Getterek és setterek
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}