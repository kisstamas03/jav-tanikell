package com.melearning.elearning.controller;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
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
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    /**
     * Főbb dashboard – átirányítja a felhasználót a szerepkörének megfelelő oldalra
     */
    @GetMapping
    public String dashboard(Authentication auth) {
        if (auth == null) return "redirect:/login";

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();
        return switch (user.getRole()) {
            case ADMIN      -> "redirect:/dashboard/admin";
            case INSTRUCTOR -> "redirect:/dashboard/instructor";
            case STUDENT    -> "redirect:/dashboard/student";
        };
    }

    // ─────────────────────────────────────────────────────────
    // ADMIN DASHBOARD
    // ─────────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String adminDashboard(Model model, Authentication auth) {
        if (!hasRole(auth, Role.ADMIN)) return "redirect:/dashboard";

        List<User> allUsers   = userService.getAllUsers();
        List<Course> allCourses = courseService.getAllCourses();

        long studentCount    = allUsers.stream().filter(u -> u.getRole() == Role.STUDENT).count();
        long instructorCount = allUsers.stream().filter(u -> u.getRole() == Role.INSTRUCTOR).count();
        long adminCount      = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();

        model.addAttribute("allUsers",       allUsers);
        model.addAttribute("allCourses",     allCourses);
        model.addAttribute("studentCount",   studentCount);
        model.addAttribute("instructorCount",instructorCount);
        model.addAttribute("adminCount",     adminCount);
        model.addAttribute("totalEnrollments",
                allCourses.stream().mapToLong(c -> c.getEnrolledUsers().size()).sum());
        model.addAttribute("currentUser",    getCurrentUser(auth));
        return "dashboard/admin";
    }

    /** Admin: felhasználó törlése */
    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes ra) {
        if (!hasRole(auth, Role.ADMIN)) return "redirect:/dashboard";

        Optional<User> self = userService.getUserByUsername(auth.getName());
        if (self.isPresent() && self.get().getId().equals(id)) {
            ra.addFlashAttribute("error", "Nem törölheted saját magadat!");
            return "redirect:/dashboard/admin";
        }
        userService.deleteUser(id);
        ra.addFlashAttribute("success", "Felhasználó sikeresen törölve.");
        return "redirect:/dashboard/admin";
    }

    /** Admin: kurzus törlése */
    @PostMapping("/admin/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id,
                               Authentication auth,
                               RedirectAttributes ra) {
        if (!hasRole(auth, Role.ADMIN)) return "redirect:/dashboard";
        courseService.deleteCourse(id);
        ra.addFlashAttribute("success", "Kurzus sikeresen törölve.");
        return "redirect:/dashboard/admin";
    }

    // ─────────────────────────────────────────────────────────
    // INSTRUCTOR DASHBOARD
    // ─────────────────────────────────────────────────────────

    @GetMapping("/instructor")
    public String instructorDashboard(Model model, Authentication auth) {
        if (!hasRole(auth, Role.INSTRUCTOR)) return "redirect:/dashboard";

        User instructor = getCurrentUser(auth);
        List<Course> myCourses = courseService.getCoursesByInstructor(instructor);

        long totalStudents   = myCourses.stream()
                .mapToLong(c -> c.getEnrolledUsers().size()).sum();
        long publicCourses   = myCourses.stream().filter(Course::getIsPublic).count();
        long privateCourses  = myCourses.stream().filter(c -> !c.getIsPublic()).count();

        model.addAttribute("currentUser",    instructor);
        model.addAttribute("myCourses",      myCourses);
        model.addAttribute("totalStudents",  totalStudents);
        model.addAttribute("publicCourses",  publicCourses);
        model.addAttribute("privateCourses", privateCourses);
        return "dashboard/instructor";
    }

    // ─────────────────────────────────────────────────────────
    // STUDENT DASHBOARD
    // ─────────────────────────────────────────────────────────

    @GetMapping("/student")
    public String studentDashboard(Model model, Authentication auth) {
        if (!hasRole(auth, Role.STUDENT)) return "redirect:/dashboard";

        User student = getCurrentUser(auth);
        List<Course> enrolled  = courseService.getEnrolledCourses(student);
        List<Course> available = courseService.getPublicCourses();
        // Ne mutassuk azokat, amelyekbe már beiratkozott
        available.removeAll(enrolled);

        model.addAttribute("currentUser",       student);
        model.addAttribute("enrolledCourses",   enrolled);
        model.addAttribute("availableCourses",  available);
        return "dashboard/student";
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private User getCurrentUser(Authentication auth) {
        return userService.getUserByUsername(auth.getName()).orElse(null);
    }

    private boolean hasRole(Authentication auth, Role role) {
        if (auth == null) return false;
        Optional<User> u = userService.getUserByUsername(auth.getName());
        return u.isPresent() && u.get().getRole() == role;
    }
}
