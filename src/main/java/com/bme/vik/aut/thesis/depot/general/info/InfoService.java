package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface InfoService {
    UserResponse getUserInfoByName(String username) throws UsernameNotFoundException;
}
