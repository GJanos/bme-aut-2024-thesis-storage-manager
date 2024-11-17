package com.bme.vik.aut.thesis.depot.general.user;

import com.bme.vik.aut.thesis.depot.security.user.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<MyUser, Long> {
    boolean existsByUserName(String userName);
    Optional<MyUser> findByUserName(String userName);
}
