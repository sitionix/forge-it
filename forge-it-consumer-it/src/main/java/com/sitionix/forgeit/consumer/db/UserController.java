package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final PostgresRepository postgresRepository;

    @PostMapping("/register")
    public ResponseEntity<UserEntity> login(@RequestBody final UserEntity request) {
        return ResponseEntity.ok(this.postgresRepository.createPostgres(request));
    }
}
