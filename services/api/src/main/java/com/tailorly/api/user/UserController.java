package com.tailorly.api.user;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.user.dto.CreateUserRequest;
import com.tailorly.api.user.dto.UpdateUserRequest;
import com.tailorly.api.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
    return userService.create(request);
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public List<UserResponse> getAllUsers() {
    return userService.findAll();
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public UserResponse getUserById(@PathVariable UUID id) {
    return userService.findById(id);
  }

  @GetMapping("/by-email")
  public UserResponse getUserByEmail(@RequestParam String email) {
    return userService.findByEmail(email);
  }

  @PutMapping("/{id}")
  public UserResponse updateUser(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateUserRequest request
  ) {
    return userService.update(id, request);
  }

  @PatchMapping("/{id}/disable")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disableUser(@PathVariable UUID id) {
    userService.disable(id);
  }
}
