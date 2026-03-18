package com.melearning.elearning.controller;

import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.EmailService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") User user,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "register";
        }

        // Ellenőrizzük, hogy a felhasználónév már létezik-e
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "A felhasználónév már foglalt!");
            return "register";
        }

        // Ellenőrizzük, hogy az email már létezik-e
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Az email cím már használatban van!");
            return "register";
        }


        try {
            userService.saveUser(user);
            // ← Email küldés regisztráció után
            emailService.sendRegistrationEmail(user.getEmail(), user.getFirstName());
            redirectAttributes.addFlashAttribute("success", "Sikeres regisztráció! Most már bejelentkezhetsz.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Hiba történt a regisztráció során. Kérjük próbáld újra!");
            return "register";
        }
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "login";
    }
}