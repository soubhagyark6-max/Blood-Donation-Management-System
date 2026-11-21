package com.lifelink.controller;

import com.lifelink.exception.BusinessException;
import com.lifelink.exception.NotFoundException;
import com.lifelink.model.*;
import com.lifelink.repository.UserRepository;
import com.lifelink.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Values every page needs (logged-in user, unread count, drop-down lists) and friendly error pages. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final UserRepository users;
    private final NotificationService notifications;

    public GlobalModelAdvice(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @ModelAttribute
    public void common(Model model, Authentication auth) {
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("components", ComponentType.values());
        model.addAttribute("urgencies", Urgency.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("contactPrefs", ContactPreference.values());
        model.addAttribute("requestStatuses", RequestStatus.values());
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            users.findByEmailIgnoreCase(auth.getName()).ifPresent(u -> {
                model.addAttribute("currentUser", u);
                model.addAttribute("unreadCount", notifications.unreadCount(u.getId()));
            });
        }
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(NotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BusinessException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/4xx";
    }
}
