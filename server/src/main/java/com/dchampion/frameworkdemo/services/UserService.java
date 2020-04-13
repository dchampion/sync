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

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User authenticate(User candidate) {
        List<User> users = userRepository.findAll(probe(candidate));
        if (users.size() == 1) {
            User user = users.get(0);
            if (getEncoder().matches(candidate.getPassword(), user.getPassword())) {
                return user;
            }
        }
        return null;
    }

    public void add(User user) {
        if (!exists(user)) {
            String encoded = getEncoder().encode(user.getPassword());
            user.setPassword(encoded);
            userRepository.save(user);
        }
    }

    public void delete(long id) {
        User user = new User();
        user.setId(id);
        userRepository.delete(user);
    }

    public boolean exists(User user) {
        return userRepository.exists(probe(user));
    }

    private Example<User> probe(User user) {
        User probe = new User();
        probe.setUsername(user.getUsername());
        return Example.of(probe);
    }

    public boolean isBreached(User user) {
        boolean isBreached = false;
        try {
            MessageDigest md = MessageDigest.getInstance(props.getBreachApiHashAlgo());
            char[] chars = Hex.encode(md.digest(user.getPassword().getBytes()));

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