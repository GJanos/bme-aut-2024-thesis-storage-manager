package com.bme.vik.aut.thesis.depot.general.info;

import com.bme.vik.aut.thesis.depot.exception.UserNotFoundByIDException;
import com.bme.vik.aut.thesis.depot.general.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/info")
@RequiredArgsConstructor
public class InfoController {

    private final InfoService infoService;

    @GetMapping("/user/me")
    public ResponseEntity<UserResponse> getUserInfo(Authentication authentication) throws UserNotFoundByIDException {

        UserResponse userResponse = infoService.getCurrentUserInfo(authentication.getName());

        return ResponseEntity.ok(userResponse);
    }
}