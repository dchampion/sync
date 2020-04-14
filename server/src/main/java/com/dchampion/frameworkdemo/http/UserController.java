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

/**
 * A {@link RestController} that manages user registration and authentication.
 */
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

    /**
     * Returns a list of all registered {@link User}s, or an empty list if none exists.
     *
     * @return a list of all registered {@link User}s, or an empty list if none exists.
     */
    @GetMapping
    public ResponseEntity<List<User>> users() {
        return ResponseEntity.ok(userService.getAll());
    }

    /**
     * Registers the supplied {@link User}, allowing subsequent authentication given
     * valid credentials.
     * <p>
     * Registration is allowed if the username embedded in the request has not already
     * been registered, and if the password embedded in the request has not been leaked
     * in a known data breach; otherwise, registration fails.
     *
     * @param user the {@link User} to register.
     *
     * @return {@link HttpStatus#OK} if the registration is successful;
     * otherwise {@link HttpStatus#BAD_REQUEST} if the user already exists
     * or {@link HttpStatus#FORBIDDEN} if the supplied password has been
     * breached.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        if (userService.exists(user.getUsername())) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(userExists);
        }
        if (userService.isBreached(user.getPassword())) {
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

    /**
     * Authenticates the supplied {@link User}, given valid credentials (i.e.
     * a username corresponding to an existing user in the datastore and a valid
     * password for that user).
     *
     * @param candidate the {@link User} to authenticate.
     *
     * @return The authenticated {@link User} if the request is successful; otherwise
     * {@link HttpStatus#BAD_REQUEST} if the user does not exist, or
     * {@link HttpStatus#UNAUTHORIZED} if the password is invalid.
     */
    @PostMapping("/authenticate")
    public ResponseEntity<Object> authenticate(@RequestBody User candidate) {
        if (!userService.exists(candidate.getUsername())) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(userDoesNotExist);
        }
        User user = userService.get(candidate.getUsername(), candidate.getPassword());
        if (user != null) {
            return ResponseEntity.ok().body(user);
        }
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            .body(invalidPassword);
    }

    /**
     * Removes the {@link User} corresponding to the supplied username from the
     * datastore.
     *
     * @param username The username of the {@link User} to remove from the datastore.
     */
    @DeleteMapping("/{username}")
    public void delete(@PathVariable String username) {
        userService.delete(username);
    }
}
