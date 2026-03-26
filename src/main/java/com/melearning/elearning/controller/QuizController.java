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

    @Autowired private CourseService courseService;
    @Autowired private QuizService quizService;
    @Autowired private UserService userService;
    @Autowired private com.melearning.elearning.repository.QuizResultRepository quizResultRepository;
    @Autowired private com.melearning.elearning.repository.QuestionRepository questionRepository;

    private boolean isOwnerOrAdmin(Course course, User user) {
        return user.getRole() == Role.ADMIN
                || course.getInstructor().getId().equals(user.getId());
    }

    // ── Kvíz létrehozás ───────────────────────────────────────────────────

    @GetMapping("/create")
    public String createQuizForm(@PathVariable Long courseId, Model model) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isEmpty()) return "redirect:/courses";
        model.addAttribute("course", courseOpt.get());
        model.addAttribute("quiz", new Quiz());
        return "courses/create-quiz";
    }

    @PostMapping("/create")
    public String saveQuiz(@PathVariable Long courseId,
                           @ModelAttribute Quiz quiz,
                           RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isPresent()) {
            quiz.setCourse(courseOpt.get());
            if (quiz.getAllowMultipleAttempts() == null) quiz.setAllowMultipleAttempts(false);
            quizService.saveQuiz(quiz);
            redirectAttributes.addFlashAttribute("success", "Kérdéssor sikeresen hozzáadva!");
            return "redirect:/courses/" + courseId + "/manage";
        }
        redirectAttributes.addFlashAttribute("error", "Hiba történt a mentésekor.");
        return "redirect:/courses";
    }

    // ── Kérdéssor törlése ─────────────────────────────────────────────────

    @PostMapping("/{quizId}/delete")
    public String deleteQuiz(@PathVariable Long courseId,
                             @PathVariable Long quizId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        Optional<User>   userOpt   = userService.getUserByUsername(auth.getName());

        if (courseOpt.isEmpty() || quizOpt.isEmpty() || userOpt.isEmpty())
            return "redirect:/courses";

        if (!isOwnerOrAdmin(courseOpt.get(), userOpt.get())) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez!");
            return "redirect:/courses/" + courseId + "/manage";
        }

        quizService.deleteQuiz(quizId);
        redirectAttributes.addFlashAttribute("success", "Kérdéssor sikeresen törölve!");
        return "redirect:/courses/" + courseId + "/manage";
    }

    // ── Ponthatárok szerkesztése ──────────────────────────────────────────

    @GetMapping("/{quizId}/thresholds")
    public String editThresholds(@PathVariable Long courseId,
                                 @PathVariable Long quizId,
                                 Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        if (courseOpt.isEmpty() || quizOpt.isEmpty()) return "redirect:/courses";

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        if (!isOwnerOrAdmin(courseOpt.get(), userOpt.get()))
            return "redirect:/courses/" + courseId;

        model.addAttribute("course", courseOpt.get());
        model.addAttribute("quiz", quizOpt.get());
        return "courses/quiz-thresholds";
    }

    /**
     * Ponthatárok mentése után az összes eddigi QuizResult jegyét újraszámítja
     * az új határok alapján — így a diákok jegye automatikusan frissül.
     */
    @PostMapping("/{quizId}/thresholds")
    public String saveThresholds(@PathVariable Long courseId,
                                 @PathVariable Long quizId,
                                 @RequestParam Integer excellentThreshold,
                                 @RequestParam Integer goodThreshold,
                                 @RequestParam Integer averageThreshold,
                                 @RequestParam Integer passingThreshold,
                                 @RequestParam(defaultValue = "false") Boolean allowMultipleAttempts,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {

        Optional<Quiz> quizOpt = quizService.getQuizById(quizId);
        if (quizOpt.isEmpty()) return "redirect:/courses/" + courseId + "/manage";

        Quiz quiz = quizOpt.get();

        if (excellentThreshold > goodThreshold
                && goodThreshold > averageThreshold
                && averageThreshold > passingThreshold
                && passingThreshold >= 0
                && excellentThreshold <= 100) {

            // 1. Új határok elmentése
            quiz.setExcellentThreshold(excellentThreshold);
            quiz.setGoodThreshold(goodThreshold);
            quiz.setAverageThreshold(averageThreshold);
            quiz.setPassingThreshold(passingThreshold);
            quiz.setAllowMultipleAttempts(allowMultipleAttempts);
            quizService.saveQuiz(quiz);

            // 2. Összes korábbi eredmény jegyének újraszámítása
            List<QuizResult> results = quizResultRepository.findByQuiz(quiz);
            for (QuizResult result : results) {
                result.setGrade(quiz.calculateGrade(result.getPercentage()));
            }
            quizResultRepository.saveAll(results);

            if (!results.isEmpty()) {
                redirectAttributes.addFlashAttribute("success",
                        "Beállítások mentve! " + results.size() + " korábbi eredmény jegye újraszámítva.");
            } else {
                redirectAttributes.addFlashAttribute("success", "Beállítások sikeresen mentve!");
            }

        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Érvénytelen ponthatárok! Csökkenő sorrendnek kell lenniük 0–100 között.");
        }

        return "redirect:/courses/" + courseId + "/quizzes/" + quizId + "/thresholds";
    }

    // ── Teszt kitöltése ───────────────────────────────────────────────────

    @GetMapping("/{quizId}/take")
    public String takeQuiz(@PathVariable Long courseId, @PathVariable Long quizId,
                           Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        if (courseOpt.isEmpty() || quizOpt.isEmpty()) return "redirect:/courses";

        Quiz quiz = quizOpt.get();

        if (auth != null && auth.isAuthenticated()) {
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean alreadyDone    = quizResultRepository.existsByUserAndQuiz(user, quiz);
                boolean onlyOneAttempt = !Boolean.TRUE.equals(quiz.getAllowMultipleAttempts());

                if (alreadyDone && onlyOneAttempt) {
                    Optional<QuizResult> prevResult = quizResultRepository
                            .findTopByUserAndQuizOrderByCompletedAtDesc(user, quiz);
                    if (prevResult.isPresent()) {
                        QuizResult r = prevResult.get();
                        int e = quiz.getExcellentThreshold(), g = quiz.getGoodThreshold(),
                                a = quiz.getAverageThreshold(),  p = quiz.getPassingThreshold();
                        model.addAttribute("course",         courseOpt.get());
                        model.addAttribute("quiz",           quiz);
                        model.addAttribute("score",          r.getScore());
                        model.addAttribute("totalQuestions", r.getTotalQuestions());
                        model.addAttribute("percentage",     r.getPercentage());
                        model.addAttribute("grade",          r.getGrade());
                        model.addAttribute("goodRange",      g + " – " + (e - 1) + "%");
                        model.addAttribute("averageRange",   a + " – " + (g - 1) + "%");
                        model.addAttribute("passingRange",   p + " – " + (a - 1) + "%");
                        model.addAttribute("alreadyCompleted", true);
                        return "courses/quiz-result";
                    }
                }
            }
        }

        model.addAttribute("course",    courseOpt.get());
        model.addAttribute("quiz",      quiz);
        model.addAttribute("questions", questionRepository.findByQuiz(quiz));
        return "courses/take-quiz";
    }

    // ── Teszt beküldése ───────────────────────────────────────────────────

    @PostMapping("/{quizId}/submit")
    public String submitQuiz(@PathVariable Long courseId,
                             @PathVariable Long quizId,
                             @RequestParam java.util.Map<String, String> allParams,
                             Authentication auth,
                             Model model) {

        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (quizOpt.isEmpty() || courseOpt.isEmpty()) return "redirect:/courses/" + courseId;

        Quiz quiz = quizOpt.get();
        List<Question> questions = questionRepository.findByQuiz(quiz);

        int score = 0;
        for (Question q : questions) {
            String submitted = allParams.get("question_" + q.getId());
            if (submitted != null && submitted.equals(q.getCorrectOption())) score++;
        }

        int totalQuestions = questions.size();
        int percentage = totalQuestions > 0
                ? (int) Math.round(((double) score / totalQuestions) * 100) : 0;
        int grade = quiz.calculateGrade(percentage);

        int e = quiz.getExcellentThreshold(), g = quiz.getGoodThreshold(),
                a = quiz.getAverageThreshold(),  p = quiz.getPassingThreshold();

        if (auth != null && auth.isAuthenticated()) {
            Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
            if (currentUserOpt.isPresent()) {
                QuizResult result = new QuizResult();
                result.setUser(currentUserOpt.get());
                result.setQuiz(quiz);
                result.setScore(score);
                result.setTotalQuestions(totalQuestions);
                result.setPercentage(percentage);
                result.setGrade(grade);
                quizResultRepository.save(result);
            }
        }

        model.addAttribute("course",         courseOpt.get());
        model.addAttribute("quiz",           quiz);
        model.addAttribute("score",          score);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("percentage",     percentage);
        model.addAttribute("grade",          grade);
        model.addAttribute("goodRange",      g + " – " + (e - 1) + "%");
        model.addAttribute("averageRange",   a + " – " + (g - 1) + "%");
        model.addAttribute("passingRange",   p + " – " + (a - 1) + "%");
        model.addAttribute("alreadyCompleted", false);
        return "courses/quiz-result";
    }

    // ── Eredmények (oktató) ───────────────────────────────────────────────

    @GetMapping("/{quizId}/results")
    public String viewResults(@PathVariable Long courseId,
                              @PathVariable Long quizId,
                              Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        if (courseOpt.isEmpty() || quizOpt.isEmpty()) return "redirect:/courses";

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        if (!isOwnerOrAdmin(courseOpt.get(), userOpt.get()))
            return "redirect:/courses/" + courseId;

        Quiz quiz = quizOpt.get();
        List<QuizResult> results = quizResultRepository.findByQuiz(quiz);

        int totalAttempts = results.size();
        String avgPct = "–", avgGrade = "–", passRatio = "0/" + totalAttempts;

        if (totalAttempts > 0) {
            double pctSum   = results.stream().mapToInt(QuizResult::getPercentage).average().orElse(0);
            double gradeSum = results.stream().mapToInt(QuizResult::getGrade).average().orElse(0);
            long passed     = results.stream().filter(r -> r.getGrade() >= 2).count();
            avgPct    = String.format("%.1f%%", pctSum);
            avgGrade  = String.format("%.2f", gradeSum);
            passRatio = passed + "/" + totalAttempts;
        }

        model.addAttribute("course",        courseOpt.get());
        model.addAttribute("quiz",          quiz);
        model.addAttribute("results",       results);
        model.addAttribute("totalAttempts", totalAttempts);
        model.addAttribute("avgPct",        avgPct);
        model.addAttribute("avgGrade",      avgGrade);
        model.addAttribute("passRatio",     passRatio);
        return "courses/quiz-results";
    }
}