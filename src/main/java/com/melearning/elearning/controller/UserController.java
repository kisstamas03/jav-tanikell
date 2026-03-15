package com.melearning.elearning.controller;

import com.melearning.elearning.model.User;
import com.melearning.elearning.service.CourseService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private CourseService courseService;

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

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}