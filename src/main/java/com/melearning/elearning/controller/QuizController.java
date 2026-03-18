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

    // ── Kvíz létrehozó oldal ──────────────────────────────────────────────

    @GetMapping("/create")
    public String createQuizForm(@PathVariable Long courseId, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isEmpty()) return "redirect:/courses";
        model.addAttribute("course", courseOpt.get());
        model.addAttribute("quiz", new Quiz());
        return "courses/create-quiz";
    }

    @PostMapping("/create")
    public String saveQuiz(@PathVariable Long courseId,
                           @ModelAttribute Quiz quiz,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        if (courseOpt.isPresent()) {
            quiz.setCourse(courseOpt.get());
            // Ha a checkbox nincs bejelölve, null-t küld a form → false-ra állítjuk
            if (quiz.getAllowMultipleAttempts() == null) {
                quiz.setAllowMultipleAttempts(false);
            }
            quizService.saveQuiz(quiz);
            redirectAttributes.addFlashAttribute("success", "Kérdéssor sikeresen hozzáadva a kurzushoz!");
            return "redirect:/courses/" + courseId + "/manage";
        }
        redirectAttributes.addFlashAttribute("error", "Hiba történt a kérdéssor mentésekor.");
        return "redirect:/courses";
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
        User user = userOpt.get();
        Course course = courseOpt.get();
        if (!course.getInstructor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN)
            return "redirect:/courses/" + courseId;

        model.addAttribute("course", course);
        model.addAttribute("quiz", quizOpt.get());
        return "courses/quiz-thresholds";
    }

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
            quiz.setExcellentThreshold(excellentThreshold);
            quiz.setGoodThreshold(goodThreshold);
            quiz.setAverageThreshold(averageThreshold);
            quiz.setPassingThreshold(passingThreshold);
            quiz.setAllowMultipleAttempts(allowMultipleAttempts);
            quizService.saveQuiz(quiz);
            redirectAttributes.addFlashAttribute("success", "Beállítások sikeresen mentve!");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Érvénytelen ponthatárok! A határoknak 0–100 közt kell lenniük, csökkenő sorrendben.");
        }
        return "redirect:/courses/" + courseId + "/quizzes/" + quizId + "/thresholds";
    }

    // ── Teszt kitöltése (blokkolás ha már kitöltötte és csak 1x engedélyezett) ──

    @GetMapping("/{quizId}/take")
    public String takeQuiz(@PathVariable Long courseId, @PathVariable Long quizId,
                           Model model, Authentication auth,
                           RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);

        if (courseOpt.isEmpty() || quizOpt.isEmpty()) return "redirect:/courses";

        Quiz quiz = quizOpt.get();

        // Ellenőrzés: már kitöltötte-e, és csak 1x engedélyezett?
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> userOpt = userService.getUserByUsername(auth.getName());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                boolean alreadyDone = quizResultRepository.existsByUserAndQuiz(user, quiz);
                boolean onlyOneAttempt = !Boolean.TRUE.equals(quiz.getAllowMultipleAttempts());

                if (alreadyDone && onlyOneAttempt) {
                    // Megmutatjuk az előző eredményt
                    Optional<QuizResult> prevResult = quizResultRepository
                            .findTopByUserAndQuizOrderByCompletedAtDesc(user, quiz);

                    if (prevResult.isPresent()) {
                        QuizResult r = prevResult.get();
                        int e = quiz.getExcellentThreshold();
                        int g = quiz.getGoodThreshold();
                        int a = quiz.getAverageThreshold();
                        int p = quiz.getPassingThreshold();

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

        List<Question> questions = questionRepository.findByQuiz(quiz);
        model.addAttribute("course",    courseOpt.get());
        model.addAttribute("quiz",      quiz);
        model.addAttribute("questions", questions);
        return "courses/take-quiz";
    }

    // ── Teszt beküldése és kiértékelése ───────────────────────────────────

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
        int totalQuestions = questions.size();
        for (Question q : questions) {
            String submitted = allParams.get("question_" + q.getId());
            if (submitted != null && submitted.equals(q.getCorrectOption())) score++;
        }

        int percentage = totalQuestions > 0
                ? (int) Math.round(((double) score / totalQuestions) * 100)
                : 0;
        int grade = quiz.calculateGrade(percentage);

        int e = quiz.getExcellentThreshold();
        int g = quiz.getGoodThreshold();
        int a = quiz.getAverageThreshold();
        int p = quiz.getPassingThreshold();

        String goodRange    = g + " – " + (e - 1) + "%";
        String averageRange = a + " – " + (g - 1) + "%";
        String passingRange = p + " – " + (a - 1) + "%";

        // Eredmény mentése (jeggyel együtt)
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
            if (currentUserOpt.isPresent()) {
                QuizResult result = new QuizResult();
                result.setUser(currentUserOpt.get());
                result.setQuiz(quiz);
                result.setScore(score);
                result.setTotalQuestions(totalQuestions);
                result.setPercentage(percentage);
                result.setGrade(grade);   // ← jegy elmentése
                quizResultRepository.save(result);
            }
        }

        model.addAttribute("course",         courseOpt.get());
        model.addAttribute("quiz",           quiz);
        model.addAttribute("score",          score);
        model.addAttribute("totalQuestions", totalQuestions);
        model.addAttribute("percentage",     percentage);
        model.addAttribute("grade",          grade);
        model.addAttribute("goodRange",      goodRange);
        model.addAttribute("averageRange",   averageRange);
        model.addAttribute("passingRange",   passingRange);
        model.addAttribute("alreadyCompleted", false);

        return "courses/quiz-result";
    }

    // ── Eredmények megtekintése (oktató) ──────────────────────────────────

    @GetMapping("/{quizId}/results")
    public String viewResults(@PathVariable Long courseId,
                              @PathVariable Long quizId,
                              Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(courseId);
        Optional<Quiz>   quizOpt   = quizService.getQuizById(quizId);
        if (courseOpt.isEmpty() || quizOpt.isEmpty()) return "redirect:/courses";

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";
        User user = userOpt.get();
        Course course = courseOpt.get();

        // Csak oktató vagy admin láthatja
        if (!course.getInstructor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN)
            return "redirect:/courses/" + courseId;

        Quiz quiz = quizOpt.get();
        List<QuizResult> results = quizResultRepository.findByQuiz(quiz);

        // Statisztikák Java oldalon kiszámítva (Thymeleaf-ben ne kelljen)
        int totalAttempts = results.size();
        String avgPct = "–";
        String avgGrade = "–";
        String passRatio = "0/" + totalAttempts;

        if (totalAttempts > 0) {
            double pctSum   = results.stream().mapToInt(QuizResult::getPercentage).average().orElse(0);
            double gradeSum = results.stream().mapToInt(QuizResult::getGrade).average().orElse(0);
            long passed     = results.stream().filter(r -> r.getGrade() >= 2).count();
            avgPct   = String.format("%.1f%%", pctSum);
            avgGrade = String.format("%.2f", gradeSum);
            passRatio = passed + "/" + totalAttempts;
        }

        model.addAttribute("course",       course);
        model.addAttribute("quiz",         quiz);
        model.addAttribute("results",      results);
        model.addAttribute("totalAttempts",totalAttempts);
        model.addAttribute("avgPct",       avgPct);
        model.addAttribute("avgGrade",     avgGrade);
        model.addAttribute("passRatio",    passRatio);
        return "courses/quiz-results";
    }
}