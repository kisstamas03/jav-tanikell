package com.melearning.elearning.controller;

import com.melearning.elearning.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "index";
    }
}
