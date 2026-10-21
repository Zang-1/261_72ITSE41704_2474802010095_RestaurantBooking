package vn.edu.vlu.restaurant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.vlu.restaurant.domain.User;
import vn.edu.vlu.restaurant.dto.CurrentUserResponse;
import vn.edu.vlu.restaurant.exception.NotFoundException;
import vn.edu.vlu.restaurant.repository.UserRepository;

/** Information about the logged-in user (UC-01). Password checking itself is done by Spring Security. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CurrentUserResponse currentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new CurrentUserResponse(user.getUsername(), user.getRole());
    }
}
