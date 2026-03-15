package com.melearning.elearning.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "presentations")
public class Presentation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] fileData;

    private String contentType;

    private Integer orderIndex;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDateTime createdAt;

    public Presentation() {
        this.createdAt = LocalDateTime.now();
    }

    public Presentation(String fileName, byte[] fileData, String contentType, Course course, Integer orderIndex) {
        this();
        this.fileName = fileName;
        this.fileData = fileData;
        this.contentType = contentType;
        this.course = course;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}