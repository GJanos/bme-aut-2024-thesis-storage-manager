package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import com.bme.vik.aut.thesis.depot.AbstractTestcontainersTest;
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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@SpringJUnitConfig(UserRepositoryTest.TestConfig.class)
class UserRepositoryTest extends AbstractTestcontainersTest {

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

    @Test
    void shouldReturnUserWhenFoundByUserName() {
        //***** <-- given --> *****//
        String userName = "depotuser";
        String password = "depotpassword";
        MyUser user = MyUser.builder()
                .userName(userName)
                .password(password)
                .build();
        userRepository.save(user);

        //***** <-- when --> *****//
        Optional<MyUser> userFound = userRepository.findByUserName(userName);

        //***** <-- then --> *****//
        assertThat(userFound).isPresent();
        assertThat(userFound.get().getUsername()).isEqualTo(userName);
        assertThat(userFound.get().getPassword()).isEqualTo(password);
    }

    @Test
    void shouldNotReturnUserWhenNotFoundByUserName() {
        //***** <-- when --> *****//
        Optional<MyUser> userFound = userRepository.findByUserName("nonexistentdepotus");

        //***** <-- then --> *****//
        assertThat(userFound).isNotPresent();
    }

    @Test
    void shouldReturnTrueWhenUserExistsByUserName() {
        //***** <-- given --> *****//
        String userName = "depotuser";
        String password = "depotpassword";
        MyUser user = MyUser.builder()
                .userName(userName)
                .password(password)
                .build();
        userRepository.save(user);

        //***** <-- when --> *****//
        boolean exists = userRepository.existsByUserName(userName);

        //***** <-- then --> *****//
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExistByUserName() {
        //***** <-- when --> *****//
        boolean exists = userRepository.existsByUserName("nonexistentdepotuser");

        //***** <-- then --> *****//
        assertThat(exists).isFalse();
    }
}