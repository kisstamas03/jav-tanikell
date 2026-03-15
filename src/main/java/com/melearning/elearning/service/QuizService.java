package com.melearning.elearning.service;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Quiz;
import com.melearning.elearning.repository.QuizRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    public Quiz saveQuiz(Quiz quiz) {
        // Fontos: Beállítjuk a kétirányú kapcsolatot a kérdések és a kvíz között
        if (quiz.getQuestions() != null) {
            quiz.getQuestions().forEach(question -> question.setQuiz(quiz));
        }
        return quizRepository.save(quiz);
    }

    public List<Quiz> getQuizzesByCourse(Course course) {
        return quizRepository.findByCourse(course);
    }

    public Optional<Quiz> getQuizById(Long id) {
        return quizRepository.findById(id);
    }

    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }
}