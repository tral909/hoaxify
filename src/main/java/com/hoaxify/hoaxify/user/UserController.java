package com.hoaxify.hoaxify.user;

import com.hoaxify.hoaxify.shared.CurrentUser;
import com.hoaxify.hoaxify.shared.GenericResponse;
import com.hoaxify.hoaxify.user.vm.UserUpdateVM;
import com.hoaxify.hoaxify.user.vm.UserVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("api/1.0")
public class UserController {

    @Autowired
    UserService userService;

    @PostMapping("users")
    GenericResponse createUser(@RequestBody @Valid User user) {
        userService.save(user);
        return new GenericResponse("User saved");
    }

    @GetMapping("users")
    Page<UserVM> getUsers(/*@PageableDefault(size = 10)*/
            @CurrentUser User loggedInUser,
            Pageable pageable) {
        return userService.getUsers(loggedInUser, pageable).map(UserVM::new);
    }

    @GetMapping("users/{username}")
    UserVM getUserByName(@PathVariable String username) {
        User user = userService.getByUsername(username);
        return new UserVM(user);
    }

    @PutMapping("/users/{id:[0-9]+}")
    @PreAuthorize("#id == principal.id")
    UserVM updateUser(@PathVariable long id, @Valid @RequestBody(required = false) UserUpdateVM userUpdate) {
        User updated = userService.update(id, userUpdate);
        return new UserVM(updated);
    }

}
