package com.hoaxify.hoaxify;

import com.hoaxify.hoaxify.error.ApiError;
import com.hoaxify.hoaxify.hoax.Hoax;
import com.hoaxify.hoaxify.hoax.HoaxRepository;
import com.hoaxify.hoaxify.hoax.HoaxService;
import com.hoaxify.hoaxify.hoax.vm.HoaxVM;
import com.hoaxify.hoaxify.user.User;
import com.hoaxify.hoaxify.user.UserRepository;
import com.hoaxify.hoaxify.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HoaxControllerTest {

    private static final String API_1_0_HOAXES = "/api/1.0/hoaxes";
    @Autowired
    TestRestTemplate testRestTemplate;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;
    @Autowired
    HoaxRepository hoaxRepository;
    @Autowired
    HoaxService hoaxService;
    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanup() {
        hoaxRepository.deleteAll();
        userRepository.deleteAll();
        testRestTemplate.getRestTemplate().getInterceptors().clear();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveOk() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveUnauthorized() {
        Hoax hoax = TestUtils.createValidHoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveApiError() {
        Hoax hoax = TestUtils.createValidHoax();
        ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDatabase() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        postHoax(hoax, Object.class);
        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDatabaseWithTimestamp() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);

        assertThat(inDB.getTimestamp()).isNotNull();
    }

    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentLessThan10CharactersAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        hoax.setContent("123456789");
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentIs5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        String veryLongString = IntStream.rangeClosed(1, 5000).mapToObj(i -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void postHoax_whenHoaxContentMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        String veryLongString = IntStream.rangeClosed(1, 5001).mapToObj(i -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveApiErrorWithValidationErrors() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = new Hoax();
        ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);
        Map<String, String> validationErrors = response.getBody().getValidationErrors();
        assertThat(validationErrors.get("content")).isNotNull();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedWithAuthenticatedUserInfo() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);

        assertThat(inDB.getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxCanBeAccessedFromUserEntity() {
        User user = userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        postHoax(hoax, Object.class);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        User inDBUser = entityManager.find(User.class, user.getId());
        assertThat(inDBUser.getHoaxes().size()).isEqualTo(1);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receiveOk() {
        ResponseEntity<Object> response = getHoaxes(new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithZeroItems() {
        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithItems() {
        User user = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());
        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithHoaxVM() {
        User user = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());
        ResponseEntity<TestPage<HoaxVM>> response = getHoaxes(new ParameterizedTypeReference<>(){});
        HoaxVM storedHoax = response.getBody().getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVM() {
        userService.save(TestUtils.createValidUser("user1"));
        authenticate("user1");
        Hoax hoax = TestUtils.createValidHoax();
        ResponseEntity<HoaxVM> response = postHoax(hoax, HoaxVM.class);
        assertThat(response.getBody().getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void postHoaxesOfUser_whenUserExists_receiveOk() {
        userService.save(TestUtils.createValidUser("user1"));
        ResponseEntity<Object> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void postHoaxesOfUser_whenUserDoesNotExist_receiveNotFound() {
        ResponseEntity<Object> response = getHoaxesOfUser("unknown-user", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void postHoaxesOfUser_whenUserExists_receivePageWithZeroHoaxes() {
        userService.save(TestUtils.createValidUser("user1"));
        ResponseEntity<TestPage<Object>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxesOfUser_whenUserExistWithHoax_receivePageWithHoaxVM() {
        User user = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(user, TestUtils.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<>(){});
        HoaxVM storedHoax = response.getBody().getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void getHoaxesOfUser_whenUserExistWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        User user = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());
        hoaxService.save(user, TestUtils.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser("user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getHoaxesOfUser_whenMultipleUserExistWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> {
            hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        });

        User userWithFiveHoaxes = userService.save(TestUtils.createValidUser("user2"));
        IntStream.rangeClosed(1, 5).forEach(i -> {
            hoaxService.save(userWithFiveHoaxes, TestUtils.createValidHoax());
        });

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(userWithFiveHoaxes.getUsername(), new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(5);
    }

    @Test
    public void getOldHoaxes_whenThereAreNoHoaxes_receiveOk() {
        ResponseEntity<Object> response = getOldHoaxes(5, new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithItemsProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<TestPage<Object>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistThereAreNoHoaxes_receiveOk() {
        userService.save(TestUtils.createValidUser("user1"));
        ResponseEntity<Object> response = getOldHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreHoaxes_receivePageWithItemsProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<TestPage<Object>> response = getOldHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserDoesNotExistThereAreNoHoaxes_receiveNotFound() {
        ResponseEntity<Object> response = getOldHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistAndThereAreNoHoaxes_receivePageWithZeroItemsBeforeProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        userService.save(TestUtils.createValidUser("user2"));

        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(fourth.getId(), "user2", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfItemsAfterProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<List<Object>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfHoaxVMAfterProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<List<HoaxVM>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistThereAreNoHoaxes_receiveOk() {
        userService.save(TestUtils.createValidUser("user1"));
        ResponseEntity<Object> response = getNewHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreHoaxes_receiveListWithItemsAfterProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<List<Object>> response = getNewHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreHoaxes_receiveListWithHoaxVMAfterProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(fourth.getId(), "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
    }


    @Test
    public void getNewHoaxesOfUser_whenUserDoesNotExistThereAreNoHoaxes_receiveNotFound() {
        ResponseEntity<Object> response = getNewHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>(){});
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistAndThereAreNoHoaxes_receiveListWithZeroItemsAfterProvidedId() {
        User userWithThreeHoaxes = userService.save(TestUtils.createValidUser("user1"));
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        Hoax fourth = hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());
        hoaxService.save(userWithThreeHoaxes, TestUtils.createValidHoax());

        userService.save(TestUtils.createValidUser("user2"));

        ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(fourth.getId(), "user2", new ParameterizedTypeReference<>(){});
        assertThat(response.getBody().size()).isEqualTo(0);
    }

    private <T> ResponseEntity<T> getNewHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> getHoaxesOfUser(String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes";
        return testRestTemplate.exchange(path, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> getHoaxes(ParameterizedTypeReference<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES, HttpMethod.GET,null, responseType);
    }

    private <T> ResponseEntity<T> postHoax(Hoax hoax, Class<T> responseType) {
        var map = new HashMap<String, Integer>();
        map.merge("1", 2, (prev, cur) -> null);
        map.entrySet().iterator().next().getValue();
        return testRestTemplate.postForEntity(API_1_0_HOAXES, hoax, responseType);
    }

    private void authenticate(String username) {
        testRestTemplate.getRestTemplate().getInterceptors().add(
                new BasicAuthenticationInterceptor(username, "P4ssword"));
    }
}
