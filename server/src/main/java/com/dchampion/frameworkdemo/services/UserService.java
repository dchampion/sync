package com.dchampion.frameworkdemo.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import com.dchampion.frameworkdemo.ConfigProps;
import com.dchampion.frameworkdemo.entities.User;
import com.dchampion.frameworkdemo.repositories.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A {@link Service} to manage the registration, authentication and retrieval
 * of {@link User}s from the datastore.
 */
@Service
public class UserService {

    @Autowired
    private ConfigProps props;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = Logger.getLogger(UserService.class.getName());

    private PasswordEncoder encoder;

    public PasswordEncoder getEncoder() {
        if (encoder == null) {
            encoder = new BCryptPasswordEncoder(props.getbCryptStrength());
        }
        return encoder;
    }

    /**
     * Returns a list of all registered users, or an empty list if none exists.
     *
     * @return a list of all registered users, or an empty list if none exists.
     */
    public List<User> getAll() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> {
            user.setPassword("");
        });
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
        List<User> users = userRepository.findAll(probe(username));
        if (users.size() == 1) {
            User user = users.get(0);
            if (getEncoder().matches(password, user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Adds the supplied {@link User} if it does not already exist in the
     * datastore. Existence is defined as a user whose username matches that
     * of the supplied {@link User}.
     *
     * @param user the {@link User} to add to the datastore.
     */
    public void add(User user) {
        if (!exists(user.getUsername())) {
            String encoded = getEncoder().encode(user.getPassword());
            user.setPassword(encoded);
            userRepository.save(user);
        }
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
    public boolean isBreached(String password) {
        boolean isBreached = false;
        try {
            MessageDigest md = MessageDigest.getInstance(props.getBreachApiHashAlgo());
            char[] chars = Hex.encode(md.digest(password.getBytes()));

            String prefix = new String(chars, 0, 5).toUpperCase();
            String url = props.getBreachApiURIRoot() + prefix;

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
            log.warning("Unsupported hash algorigthm: " + props.getBreachApiHashAlgo());
        } catch (URISyntaxException e) {
            log.warning(e.getMessage());
        }
        return isBreached;
    }
}