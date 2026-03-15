package com.melearning.elearning.controller;

import com.melearning.elearning.model.Course;
import com.melearning.elearning.model.Presentation;
import com.melearning.elearning.model.Role;
import com.melearning.elearning.model.User;
import com.melearning.elearning.service.PresentationService;
import com.melearning.elearning.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ContentDisposition;

import java.util.Optional;

@Controller
public class PresentationController {

    @Autowired
    private PresentationService presentationService;

    @Autowired
    private UserService userService;

    /**
     * Ellenőrzi, hogy az aktuális felhasználónak van-e jogosultsága
     * a prezentáció megtekintéséhez.
     *
     * Hozzáférhet:
     *  - ADMIN szerepkörű felhasználó
     *  - A kurzus oktatója
     *  - A kurzusba beiratkozott diák
     */
    private boolean hasAccessToPresentation(Presentation presentation, Authentication auth) {
        if (auth == null) {
            return false;
        }

        Optional<User> userOpt = userService.getUserByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        Course course = presentation.getCourse();

        // ADMIN mindent láthat
        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        // A kurzus oktatója láthatja
        if (course.getInstructor() != null && course.getInstructor().getId().equals(user.getId())) {
            return true;
        }

        // Beiratkozott diák láthatja
        if (course.getEnrolledUsers().contains(user)) {
            return true;
        }

        return false;
    }

    @GetMapping("/presentations/{id}/view")
    public String viewPresentation(@PathVariable Long id, Model model, Authentication auth) {
        Optional<Presentation> presentationOpt = presentationService.getPresentationById(id);

        if (presentationOpt.isEmpty()) {
            return "redirect:/courses";
        }

        Presentation presentation = presentationOpt.get();

        // Jogosultság ellenőrzés
        if (!hasAccessToPresentation(presentation, auth)) {
            return "redirect:/courses/" + presentation.getCourse().getId() + "?error=access_denied";
        }

        model.addAttribute("presentation", presentation);
        model.addAttribute("courseId", presentation.getCourse().getId());

        return "presentation/viewer";
    }

    @GetMapping("/presentations/{id}/content")
    public ResponseEntity<byte[]> getPresentationContent(@PathVariable Long id, Authentication auth) {
        Optional<Presentation> presentationOpt = presentationService.getPresentationById(id);

        if (presentationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Presentation presentation = presentationOpt.get();

        // Jogosultság ellenőrzés a tartalom letöltéséhez is
        if (!hasAccessToPresentation(presentation, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String fileName = presentation.getFileName().toLowerCase();
        HttpHeaders headers = new HttpHeaders();

        if (fileName.endsWith(".pdf")) {
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.inline().filename(presentation.getFileName()).build()
            );
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(
                    ContentDisposition.attachment().filename(presentation.getFileName()).build()
            );
        }

        return new ResponseEntity<>(presentation.getFileData(), headers, HttpStatus.OK);
    }
}