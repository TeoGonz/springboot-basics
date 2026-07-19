package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.model.Post;

@Controller
public class PageController {

    // Entradas de la bitácora. Estáticas por ahora; próxima iteración -> API.
    private static final List<Post> POSTS = List.of(
            new Post("internacionalizacion", 2, LocalDate.of(2026, 7, 18),
                    "post.i18n.title", "post.i18n.summary",
                    "i18n", "bi-translate", "text-bg-primary", true),
            new Post("guards", 1, LocalDate.of(2026, 7, 11),
                    "post.guards.title", "post.guards.summary",
                    "guards", "bi-shield-lock", "text-bg-success", true),
            new Post("proximo", 3, null,
                    "post.next.title", "post.next.summary",
                    "next", "bi-hourglass-split", "text-bg-secondary", false));

    @GetMapping("/public")
    public String publicPage(Model model) {
        model.addAttribute("posts", POSTS);
        return "public";
    }

    @GetMapping("/user")
    public String userPage() {
        return "user";
    }

    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }
}
