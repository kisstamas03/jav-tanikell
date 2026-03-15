package com.melearning.elearning.controller;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.PresentationService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private PresentationService presentationService;

    @Autowired
    private UserService userService;

    // -----------------------------------------------------------------------
    // Segédmetódusok
    // -----------------------------------------------------------------------

    /**
     * Visszaadja a bejelentkezett felhasználót, vagy null-t ha nincs.
     */
    private Optional<User> getCurrentUser(Authentication auth) {
        if (auth == null) return Optional.empty();
        return userService.getUserByUsername(auth.getName());
    }

    /**
     * Ellenőrzi, hogy a felhasználó ADMIN-e.
     */
    private boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    /**
     * Ellenőrzi, hogy a felhasználó a kurzus oktatója VAGY admin-e.
     */
    private boolean isOwnerOrAdmin(Course course, User user) {
        return isAdmin(user) || course.getInstructor().getId().equals(user.getId());
    }

    // -----------------------------------------------------------------------
    // Kurzus lista
    // -----------------------------------------------------------------------

    @GetMapping
    public String listCourses(Model model, Authentication auth) {
        List<Course> courses;
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isPresent()) {
            User currentUser = userOpt.get();

            if (currentUser.getRole() == Role.INSTRUCTOR || isAdmin(currentUser)) {
                // Oktató és admin lát mindent
                courses = courseService.getAllCourses();
            } else {
                // Diák: publikus + a saját beiratkozott privát kurzusok
                List<Course> publicCourses = courseService.getPublicCourses();
                List<Course> enrolledCourses = courseService.getEnrolledCourses(currentUser);

                courses = new ArrayList<>(publicCourses);
                for (Course enrolledCourse : enrolledCourses) {
                    if (!courses.contains(enrolledCourse)) {
                        courses.add(enrolledCourse);
                    }
                }
            }
        } else {
            courses = courseService.getPublicCourses();
        }

        model.addAttribute("courses", courses);
        return "courses/list";
    }

    // -----------------------------------------------------------------------
    // Kurzus megtekintése
    // -----------------------------------------------------------------------

    @GetMapping("/{id}")
    public String viewCourse(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        if (courseOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Course courseObj = courseOpt.get();
        Optional<User> userOpt = getCurrentUser(auth);

        // Privát kurzus: csak az oktató, admin vagy beiratkozott diák láthatja
        if (!courseObj.getIsPublic()) {
            if (userOpt.isEmpty()) {
                return "redirect:/courses";
            }
            User currentUser = userOpt.get();
            boolean canView = isOwnerOrAdmin(courseObj, currentUser)
                    || courseObj.getEnrolledUsers().contains(currentUser);
            if (!canView) {
                return "redirect:/courses";
            }
        }

        model.addAttribute("course", courseObj);
        model.addAttribute("presentations", presentationService.getPresentationsByCourse(courseObj));

        userOpt.ifPresent(user -> {
            model.addAttribute("isEnrolled", courseObj.getEnrolledUsers().contains(user));
            model.addAttribute("user", user);
        });

        if (!model.containsAttribute("isEnrolled")) {
            model.addAttribute("isEnrolled", false);
        }

        return "courses/view";
    }

    // -----------------------------------------------------------------------
    // Kurzus létrehozása
    // -----------------------------------------------------------------------

    @GetMapping("/create")
    public String createCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@Valid @ModelAttribute("course") Course course,
                               Authentication auth,
                               @RequestParam(value = "presentations", required = false) MultipartFile[] presentations,
                               RedirectAttributes redirectAttributes) {

        Optional<User> instructorOpt = getCurrentUser(auth);
        if (instructorOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Oktató nem található!");
            return "redirect:/courses/create";
        }

        User instructor = instructorOpt.get();

        // Extra ellenőrzés: csak INSTRUCTOR vagy ADMIN hozhat létre kurzust
        if (instructor.getRole() != Role.INSTRUCTOR && !isAdmin(instructor)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod kurzus létrehozásához!");
            return "redirect:/courses";
        }

        try {
            course.setInstructor(instructor);
            Course savedCourse = courseService.saveCourse(course);

            if (presentations != null) {
                int order = 1;
                for (MultipartFile file : presentations) {
                    if (!file.isEmpty()) {
                        String fileName = file.getOriginalFilename();
                        if (fileName != null && (fileName.toLowerCase().endsWith(".pdf") ||
                                fileName.toLowerCase().endsWith(".ppt") ||
                                fileName.toLowerCase().endsWith(".pptx"))) {
                            presentationService.processAndSavePresentation(file, savedCourse, order++);
                        } else {
                            redirectAttributes.addFlashAttribute("error",
                                    "Csak PDF, PPT vagy PPTX fájlokat lehet feltölteni!");
                            return "redirect:/courses/create";
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Kurzus sikeresen létrehozva!");
            return "redirect:/courses";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Hiba történt a fájl feldolgozása során: " + e.getMessage());
            return "redirect:/courses/create";
        }
    }

    // -----------------------------------------------------------------------
    // Beiratkozás
    // -----------------------------------------------------------------------

    @PostMapping("/{id}/enroll")
    public String enrollInCourse(@PathVariable Long id, Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> userOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || userOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Course course = courseOpt.get();
        User user = userOpt.get();

        // Oktató nem iratkozhat be saját kurzusára
        if (isOwnerOrAdmin(course, user)) {
            redirectAttributes.addFlashAttribute("error", "Oktatóként nem iratkozhatsz be saját kurzusodra!");
            return "redirect:/courses/" + id;
        }

        // Privát kurzusra csak az oktató adhat hozzá diákot (nem önmaga)
        if (!course.getIsPublic()) {
            redirectAttributes.addFlashAttribute("error",
                    "Ez egy privát kurzus, csak az oktató adhat hozzá diákokat!");
            return "redirect:/courses/" + id;
        }

        courseService.enrollUser(course, user);
        return "redirect:/courses/" + id;
    }

    // -----------------------------------------------------------------------
    // Kurzus kezelése (csak saját oktató vagy admin)
    // -----------------------------------------------------------------------

    @GetMapping("/{id}/manage")
    public String manageCourse(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Course> courseOpt = courseService.getCourseById(id);
        if (courseOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Course courseObj = courseOpt.get();
        Optional<User> userOpt = getCurrentUser(auth);

        if (userOpt.isEmpty() || !isOwnerOrAdmin(courseObj, userOpt.get())) {
            return "redirect:/courses/" + id;
        }

        model.addAttribute("course", courseObj);
        model.addAttribute("allStudents", userService.getAllUsers().stream()
                .filter(u -> u.getRole() == Role.STUDENT)
                .toList());
        return "courses/manage";
    }

    @PostMapping("/{id}/add-student")
    public String addStudent(@PathVariable Long id,
                             @RequestParam Long studentId,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> studentOpt = userService.getUserById(studentId);
        Optional<User> currentUserOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || studentOpt.isEmpty() || currentUserOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Course course = courseOpt.get();
        User currentUser = currentUserOpt.get();

        // Csak az oktató vagy admin adhat hozzá diákot
        if (!isOwnerOrAdmin(course, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez a művelethez!");
            return "redirect:/courses/" + id;
        }

        // Csak STUDENT szerepkörű felhasználót lehet hozzáadni
        User student = studentOpt.get();
        if (student.getRole() != Role.STUDENT) {
            redirectAttributes.addFlashAttribute("error", "Csak hallgatókat lehet beiratkoztatni!");
            return "redirect:/courses/" + id + "/manage";
        }

        courseService.enrollUser(course, student);
        redirectAttributes.addFlashAttribute("success", "Diák sikeresen hozzáadva!");
        return "redirect:/courses/" + id + "/manage";
    }

    @PostMapping("/{id}/remove-student")
    public String removeStudent(@PathVariable Long id,
                                @RequestParam Long studentId,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {

        Optional<Course> courseOpt = courseService.getCourseById(id);
        Optional<User> studentOpt = userService.getUserById(studentId);
        Optional<User> currentUserOpt = getCurrentUser(auth);

        if (courseOpt.isEmpty() || studentOpt.isEmpty() || currentUserOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Course course = courseOpt.get();
        User currentUser = currentUserOpt.get();

        // Csak az oktató vagy admin távolíthat el diákot
        if (!isOwnerOrAdmin(course, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "Nincs jogosultságod ehhez a művelethez!");
            return "redirect:/courses/" + id;
        }

        courseService.unenrollUser(course, studentOpt.get());
        redirectAttributes.addFlashAttribute("success", "Diák eltávolítva!");
        return "redirect:/courses/" + id + "/manage";
    }
}