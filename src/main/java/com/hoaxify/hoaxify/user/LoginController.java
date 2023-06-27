package com.hoaxify.hoaxify.user;

import com.hoaxify.hoaxify.shared.CurrentUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
public class LoginController {

    @PostMapping("/api/1.0/login")
    Map<String, Object> handleLogin(@CurrentUser User loggedInUser /* main approach */) {
        // second approach (authentication must be parameter in controller)
        //User loggedInUser = (User) authentication.getPrincipal();
        // first approach
        //User loggedInUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Collections.singletonMap("id", loggedInUser.getId());
    }
}
