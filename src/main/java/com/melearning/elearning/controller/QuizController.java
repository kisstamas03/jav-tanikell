package com.melearning.elearning.controller;

import com.melearning.elearning.model.*;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.QuizService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/courses/{courseId}/quizzes")
public class QuizController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.melearning.elearning.repository.QuizResultRepository quizResultRepository;

    @Autowired
    private com.melearning.elearning.repository.QuestionRepository questionRepository;

    // Megjeleníti a kvíz-készítő űrlapot
    @GetMapping("/create")
    public String createQuizForm(@PathVariable Long courseId, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isEmpty()) {
            return "redirect:/courses";
        }

        // Itt is ellenőrizhetnéd, hogy az adott felhasználó-e az oktató, ahogy a CourseControllerben tetted!

        model.addAttribute("course", courseOpt.get());
        model.addAttribute("quiz", new Quiz()); // Egy üres kvízt küldünk a formnak

        return "courses/create-quiz"; // Ezt a HTML-t fogjuk mindjárt megírni
    }

    // Megjeleníti a tesztkitöltő oldalt a diáknak
    @GetMapping("/{quizId}/take")
    public String takeQuiz(@PathVariable Long courseId, @PathVariable Long quizId, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz> quizOpt = quizService.getQuizById(quizId);

        if (courseOpt.isPresent() && quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();

            // KÜLÖN lekérdezzük a kérdéseket az adatbázisból, így garantáltan nem lesz üres!
            List<Question> questions = questionRepository.findByQuiz(quiz);

            model.addAttribute("course", courseOpt.get());
            model.addAttribute("quiz", quiz);
            model.addAttribute("questions", questions); // Átadjuk a listát a HTML-nek

            return "courses/take-quiz";
        }

        return "redirect:/courses";
    }

    // Fogadja a diák válaszait, kiértékeli, és megmutatja az eredményt
    @PostMapping("/{quizId}/submit")
    public String submitQuiz(@PathVariable Long courseId,
                             @PathVariable Long quizId,
                             @RequestParam java.util.Map<String, String> allParams,
                             Authentication auth,
                             Model model) {

        Optional<Quiz> quizOpt = quizService.getQuizById(quizId);
        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (quizOpt.isPresent() && courseOpt.isPresent()) {
            Quiz quiz = quizOpt.get();

            // Közvetlenül az adatbázisból vesszük a kérdéseket, hogy biztosan ne legyen üres a lista!
            List<com.melearning.elearning.model.Question> questions = questionRepository.findByQuiz(quiz);

            int score = 0;
            int totalQuestions = questions.size();

            // Végigmegyünk a lekérdezett kérdéseken a kiértékeléshez
            for (com.melearning.elearning.model.Question q : questions) {
                String submittedAnswer = allParams.get("question_" + q.getId());
                if (submittedAnswer != null && submittedAnswer.equals(q.getCorrectOption())) {
                    score++;
                }
            }

            // Százalék kiszámítása
            int percentage = totalQuestions > 0 ? (int) Math.round(((double) score / totalQuestions) * 100) : 0;

            // Eredmény elmentése az adatbázisba a bejelentkezett felhasználóhoz
            if (auth != null && auth.isAuthenticated()) {
                String username = auth.getName();

                // Használjuk a korábban meglévő metódusodat a UserService-ből
                Optional<com.melearning.elearning.model.User> currentUserOpt = userService.getUserByUsername(username);

                if (currentUserOpt.isPresent()) {
                    com.melearning.elearning.model.QuizResult result = new com.melearning.elearning.model.QuizResult();
                    result.setUser(currentUserOpt.get());
                    result.setQuiz(quiz);
                    result.setScore(score);
                    result.setTotalQuestions(totalQuestions);
                    result.setPercentage(percentage);

                    quizResultRepository.save(result); // Eredmény mentése
                }
            }

            // Adatok átadása az eredmény oldalnak (quiz-result.html)
            model.addAttribute("course", courseOpt.get());
            model.addAttribute("quiz", quiz);
            model.addAttribute("score", score);
            model.addAttribute("totalQuestions", totalQuestions);
            model.addAttribute("percentage", percentage);

            return "courses/quiz-result";
        }

        return "redirect:/courses/" + courseId;
    }

    // Fogadja a kitöltött űrlapot és elmenti a kvízt a kérdésekkel együtt
    @PostMapping("/create")
    public String saveQuiz(@PathVariable Long courseId,
                           @ModelAttribute Quiz quiz,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(courseId);

        if (courseOpt.isPresent()) {
            Course course = courseOpt.get();
            quiz.setCourse(course); // Hozzákötjük a kvízt a kurzushoz

            quizService.saveQuiz(quiz); // A Service elmenti a kvízt ÉS a benne lévő kérdéseket is

            System.out.println("Beérkezett kérdések száma: " + quiz.getQuestions().size());

            redirectAttributes.addFlashAttribute("success", "Kérdéssor sikeresen hozzáadva a kurzushoz!");
            return "redirect:/courses/" + courseId + "/manage";
        }

        redirectAttributes.addFlashAttribute("error", "Hiba történt a kérdéssor mentésekor.");
        return "redirect:/courses";
    }
}