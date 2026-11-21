package com.lifelink.controller;

import com.lifelink.model.User;
import com.lifelink.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class NotificationController {

    private final NotificationService notifications;
    private final ControllerSupport support;

    public NotificationController(NotificationService notifications, ControllerSupport support) {
        this.notifications = notifications;
        this.support = support;
    }

    @GetMapping("/notifications")
    public String list(Authentication auth, Model model) {
        User u = support.user(auth);
        model.addAttribute("notifications", notifications.latest(u.getId()));
        return "notifications/list";
    }

    @PostMapping("/notifications/read-all")
    public String readAll(Authentication auth) {
        notifications.markAllRead(support.user(auth).getId());
        return "redirect:/notifications";
    }
}
