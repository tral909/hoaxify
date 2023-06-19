package com.hoaxify.hoaxify;

import com.hoaxify.hoaxify.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void postUser_whenUserIsValid_receiveOk() {
        User user = new User();
        user.setUsername("test-user");
        user.setDisplayName("test-display");
        user.setPassword("P4ssword");

        ResponseEntity<Object> response = testRestTemplate.postForEntity("/api/1.0/users", user, Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
