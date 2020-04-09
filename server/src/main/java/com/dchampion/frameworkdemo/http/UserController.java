package com.dchampion.frameworkdemo.http;

import com.dchampion.frameworkdemo.entities.User;
import com.dchampion.frameworkdemo.services.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    private static final String successMessage =
        "Registration successful";

    private static final String existsMessage =
        "User already exists";

    private static final String breachedMessage =
        "The password you typed has appeared in a data breach and should not be used";

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userService.exists(user)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(existsMessage);
        }
        if (userService.isBreached(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(breachedMessage);
        }
        userService.addUser(user);
        return ResponseEntity.ok().body(successMessage);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user) {
        return null;
    }
}
