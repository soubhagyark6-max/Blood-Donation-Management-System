package com.lifelink.controller;

import com.lifelink.model.User;
import com.lifelink.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Small helper so controllers can load the logged-in User entity. */
@Component
public class ControllerSupport {

    private final UserService users;

    public ControllerSupport(UserService users) {
        this.users = users;
    }

    public User user(Authentication auth) {
        return users.getByEmail(auth.getName());
    }
}
