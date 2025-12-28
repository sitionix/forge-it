package com.sitionix.forgeit.consumer.db;

import com.sitionix.forgeit.consumer.db.dto.UserDTO;
import com.sitionix.forgeit.consumer.db.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "forge-it.modules.postgresql", name = "enabled", havingValue = "true")
public class UserController {

    private final PostgresRepository postgresRepository;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> login(@RequestBody final UserEntity request) {
        final UserDTO response = UserDTO.asUserDTO(this.postgresRepository.createPostgres(request));
        return ResponseEntity.ok(response);
    }
}
