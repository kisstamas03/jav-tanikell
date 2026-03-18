package com.melearning.elearning.repository;

import com.melearning.elearning.model.Quiz;
import com.melearning.elearning.model.QuizResult;
import com.melearning.elearning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    List<QuizResult> findByUser(User user);

    List<QuizResult> findByQuiz(Quiz quiz);

    // Egy adott user kitöltötte-e már ezt a kvízt?
    boolean existsByUserAndQuiz(User user, Quiz quiz);

    // Legutóbbi eredmény user+quiz kombinációra
    Optional<QuizResult> findTopByUserAndQuizOrderByCompletedAtDesc(User user, Quiz quiz);
}