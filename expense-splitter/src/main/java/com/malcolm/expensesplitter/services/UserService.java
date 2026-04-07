package com.malcolm.expensesplitter.services;

import com.malcolm.expensesplitter.models.User;
import com.malcolm.expensesplitter.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User getOrCreateUserFromJwt(Jwt jwt) {
        String auth0Id = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        Optional<User> existingUser = userRepository.findByAuth0Id(auth0Id);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Check by email if auth0Id is missing (e.g., legacy or migration)
        Optional<User> userByEmail = userRepository.findByEmail(email);
        if (userByEmail.isPresent()) {
            User user = userByEmail.get();
            user.setAuth0Id(auth0Id);
            return userRepository.save(user);
        }

        // New user
        User newUser = new User(name != null ? name : "New User", email, "INR");
        newUser.setAuth0Id(auth0Id);
        return userRepository.save(newUser);
    }
}
