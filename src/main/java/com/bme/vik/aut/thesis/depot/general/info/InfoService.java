package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.exception.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;

public interface InfoService {
    UserResponse getCurrentUserInfo(String username) throws UserNotFoundByIDException;
}
