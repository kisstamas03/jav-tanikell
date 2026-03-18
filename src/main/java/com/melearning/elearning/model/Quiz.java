package com.melearning.elearning.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String title;

    // Értékelési határok (%)
    private Integer excellentThreshold = 85;
    private Integer goodThreshold      = 70;
    private Integer averageThreshold   = 55;
    private Integer passingThreshold   = 40;

    // Több kitöltés engedélyezése (true = bármennyiszer, false = csak 1x)
    private Boolean allowMultipleAttempts = true;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Question> questions = new ArrayList<>();

    public Quiz() {}

    public Quiz(String title, Course course) {
        this.title = title;
        this.course = course;
    }

    public int calculateGrade(int percentage) {
        if (percentage >= excellentThreshold) return 5;
        if (percentage >= goodThreshold)      return 4;
        if (percentage >= averageThreshold)   return 3;
        if (percentage >= passingThreshold)   return 2;
        return 1;
    }

    // Getterek és Setterek
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public Integer getExcellentThreshold() { return excellentThreshold; }
    public void setExcellentThreshold(Integer excellentThreshold) { this.excellentThreshold = excellentThreshold; }

    public Integer getGoodThreshold() { return goodThreshold; }
    public void setGoodThreshold(Integer goodThreshold) { this.goodThreshold = goodThreshold; }

    public Integer getAverageThreshold() { return averageThreshold; }
    public void setAverageThreshold(Integer averageThreshold) { this.averageThreshold = averageThreshold; }

    public Integer getPassingThreshold() { return passingThreshold; }
    public void setPassingThreshold(Integer passingThreshold) { this.passingThreshold = passingThreshold; }

    public Boolean getAllowMultipleAttempts() { return allowMultipleAttempts; }
    public void setAllowMultipleAttempts(Boolean allowMultipleAttempts) { this.allowMultipleAttempts = allowMultipleAttempts; }
}