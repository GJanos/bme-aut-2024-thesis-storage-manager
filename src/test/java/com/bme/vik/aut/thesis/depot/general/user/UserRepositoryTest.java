package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.AbstractTestcontainersTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
//@Testcontainers
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
//@SpringJUnitConfig(UserRepositoryTest.TestConfig.class)
class UserRepositoryTest {

//    @TestConfiguration
//    static class TestConfig {
//
//        @Bean
//        @Primary
//        public PasswordEncoder passwordEncoder() {
//            return new BCryptPasswordEncoder();
//        }
//    }

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    private final static String USER_NAME = "depotuser";
    private final static String USER_PASSWORD = "depotuser";

    @BeforeEach
    void setUp() {
        //***** <-- given --> *****//
        MyUser user = MyUser.builder()
                .userName(USER_NAME)
                .password(USER_PASSWORD)
                .build();
        userRepository.save(user);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnUserWhenFoundByUserName() {
        //***** <-- when --> *****//
        Optional<MyUser> userFound = userRepository.findByUserName(USER_NAME);

        //***** <-- then --> *****//
        assertThat(userFound).isPresent();
        assertThat(userFound.get().getUsername()).isEqualTo(USER_NAME);
        assertThat(userFound.get().getPassword()).isEqualTo(USER_PASSWORD);
    }

    @Test
    void shouldNotReturnUserWhenNotFoundByUserName() {
        //***** <-- when --> *****//
        String userName = "nonexistentdepotuser";
        Optional<MyUser> userFound = userRepository.findByUserName(userName);

        //***** <-- then --> *****//
        assertThat(userFound).isNotPresent();
    }

    @Test
    void shouldReturnTrueWhenUserExistsByUserName() {
        //***** <-- when --> *****//
        boolean exists = userRepository.existsByUserName(USER_NAME);

        //***** <-- then --> *****//
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistByUserName() {
        //***** <-- when --> *****//
        String userName = "nonexistentdepotuser";
        boolean exists = userRepository.existsByUserName(userName);

        //***** <-- then --> *****//
        assertThat(exists).isFalse();
    }
}