package com.melearning.elearning.repository;

import com.melearning.elearning.model.Quiz;
import com.melearning.elearning.model.QuizResult;
import com.melearning.elearning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    // Ezzel később le tudod kérdezni egy diák összes eredményét:
    List<QuizResult> findByUser(User user);

    // Ezzel pedig egy adott kvíz összes kitöltését (pl. az oktatónak):
    List<QuizResult> findByQuiz(Quiz quiz);
}