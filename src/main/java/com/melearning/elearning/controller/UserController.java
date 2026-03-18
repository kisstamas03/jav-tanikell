package com.melearning.elearning.controller;

import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.EmailService;
import com.melearning.elearning.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        if (auth != null) {
            Optional<User> user = userService.getUserByUsername(auth.getName());
            if (user.isPresent()) {
                model.addAttribute("user", user.get());
            }
        }
        return "profile";
    }

    @GetMapping("/my-courses")
    public String myCourses(Model model, Authentication auth) {
        if (auth != null) {
            Optional<User> user = userService.getUserByUsername(auth.getName());
            if (user.isPresent()) {
                model.addAttribute("user", user.get());
                model.addAttribute("enrolledCourses", courseService.getEnrolledCourses(user.get()));
                model.addAttribute("teachingCourses", courseService.getCoursesByInstructor(user.get()));
            }
        }
        return "my-courses";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("pwError", "Az új jelszavak nem egyeznek!");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("pwError", "A jelszónak legalább 6 karakter hosszúnak kell lennie!");
            return "redirect:/profile";
        }

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isPresent()) {
            boolean success = userService.changePassword(userOpt.get(), currentPassword, newPassword);
            if (success) {
                redirectAttributes.addFlashAttribute("pwSuccess", "Jelszó sikeresen megváltoztatva!");
            } else {
                redirectAttributes.addFlashAttribute("pwError", "Hibás jelenlegi jelszó!");
            }
        }
        return "redirect:/profile";
    }

    @PostMapping("/delete-account")
    public String deleteAccount(Authentication auth,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) return "redirect:/login";

        User user = userOpt.get();

        // Email küldés törlés ELŐTT (törlés után már nincs adat)
        emailService.sendAccountDeletionEmail(user.getEmail(), user.getFirstName());

        // Felhasználó törlése
        userService.deleteUser(user.getId());

        // Session invalidálása – ki kell jelentkeztetni a törölt usert
        try {
            request.logout();
        } catch (Exception e) {
            System.err.println("Logout hiba törléskor: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("success",
                "Fiókod sikeresen törölve lett. Viszontlátásra!");
        return "redirect:/login";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}