package com.dchampion.frameworkdemo.http;

import java.util.List;

import com.dchampion.frameworkdemo.entities.User;
import com.dchampion.frameworkdemo.services.UserService;
import com.google.common.net.HttpHeaders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    private static final String registrationSuccessful =
        "Registration successful";

    private static final String userExists =
        "User already exists";

    private static final String userDoesNotExist =
        "User not found";

    private static final String passwordBreached =
        "The password you typed has been leaked in a data breach and should not be used";

    private static final String invalidPassword =
        "The password you typed is incorrect";

    @GetMapping
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userService.exists(user)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(userExists);
        }
        if (userService.isBreached(user)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(passwordBreached);
        }
        userService.add(user);
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            .body(registrationSuccessful);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Object> authenticate(@RequestBody User candidate) {
        if (!userService.exists(candidate)) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(userDoesNotExist);
        }
        User user = userService.authenticate(candidate);
        if (user != null) {
            return ResponseEntity.ok().body(user);
        }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            .body(invalidPassword);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        userService.delete(id);
    }
}
