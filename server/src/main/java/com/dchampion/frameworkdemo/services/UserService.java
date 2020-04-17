package com.dchampion.frameworkdemo.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import com.dchampion.frameworkdemo.entities.User;
import com.dchampion.frameworkdemo.repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A {@link Service} to manage the registration, authentication and retrieval
 * of {@link User}s from the datastore.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${framework-demo.breach-api-uri-root}")
    private String breachApiUriRoot;

    @Value("${framework-demo.breach-api-hash-algo}")
    private String breachApiHashAlgo;

    private static final Logger log = Logger.getLogger(UserService.class.getName());

    /**
     * Returns a list of all registered users, or an empty list if none exists.
     *
     * @return a list of all registered users, or an empty list if none exists.
     */
    public List<User> getAll() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> user.setPassword(""));
        return users;
    }

    /**
     * Returns the {@link User} corresponding to the supplied username/password pair
     * from the datastore, or {@code null} if the {@link User} corresponding to
     * the supplied username does not exist and/or if the supplied password is invalid.
     *
     * @param username The username of the requested {@link User}.
     * @param password The password of the requested {@link User}.
     *
     * @return The requested {@link User}, or {@code null} if the supplied
     * credentials are invalid.
     */
    public User get(String username, String password) {
        User user = null;
        List<User> users = userRepository.findAll(probe(username));
        if (users.size() == 1 && encoder.matches(password, users.get(0).getPassword())) {
            user = users.get(0);
            user.setPassword("");
        }
        return user;
    }

    /**
     * Adds the supplied {@link User} if it does not already exist in the
     * datastore. Existence is defined as a user whose username matches that
     * of the supplied {@link User}.
     *
     * @param user the {@link User} to add to the datastore.
     *
     * @return the added {@link User}, or {@code null} if the user already exists
     * or could not otherwise be saved to the datastore.
     */
    public boolean add(User user) {
        boolean added = false;
        if (!exists(user.getUsername())) {
            String encoded = encoder.encode(user.getPassword());
            user.setPassword(encoded);
            userRepository.save(user);
            added = true;
        }
        return added;
    }

    /**
     * Removes the {@link User} corresponding to the supplied username from the
     * datastore.
     *
     * @param username The username of the {@link User} to delete.
     */
    public void delete(String username) {
        List<User> users = userRepository.findAll(probe(username));
        if (users.size() == 1) {
            userRepository.delete(users.get(0));
        }
    }

    /**
     * Returns {@code true} if the supplied username matches a {@link User} containing
     * the same username in the datastore; otherwise returns {@code false}.
     *
     * @param username the {@link User} to test for existence in the datastore.
     *
     * @return {@code true} if the user exists; {@code false} otherwise.
     */
    public boolean exists(String username) {
        return userRepository.exists(probe(username));
    }

    private Example<User> probe(String username) {
        User probe = new User();
        probe.setUsername(username);
        return Example.of(probe);
    }

    /**
     * Returns {@code true} if the supplied password has been previously leaked in a
     * known data breach; otherwise returns {@code false}.
     *
     * @param password The password to test for a breach.
     *
     * @return {@code true} if the password has been leaked; {@code false} otherwise.
     */
    public boolean passwordLeaked(String password) {
        boolean isBreached = false;
        try {
            MessageDigest md = MessageDigest.getInstance(breachApiHashAlgo);
            char[] chars = Hex.encode(md.digest(password.getBytes()));

            String prefix = new String(chars, 0, 5).toUpperCase();
            String url = breachApiUriRoot + prefix;

            RestTemplate client = new RestTemplate();
            RequestEntity<Void> request =
                RequestEntity.get(new URI(url)).accept(
                    MediaType.TEXT_HTML).header("Add-Padding", "true").build();

            ResponseEntity<String> response = client.exchange(request, String.class);
            String[] body = response.getBody().split("\n");

            String suffix = new String(chars, 5, chars.length-5).toUpperCase();
            for (int i=0; i<body.length && !isBreached; i++) {
                if (body[i].startsWith(suffix) && !body[i].endsWith(":0")) {
                    isBreached = true;
                }
            }
        } catch (NoSuchAlgorithmException e) {
            log.warning("Unsupported hash algorigthm: " + breachApiHashAlgo);
        } catch (URISyntaxException e) {
            log.warning(e.getMessage());
        }
        return isBreached;
    }
}