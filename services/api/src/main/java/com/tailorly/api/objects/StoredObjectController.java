package com.tailorly.api.objects;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.objects.dto.CreateStoredObjectRequest;
import com.tailorly.api.objects.dto.StoredObjectResponse;
import com.tailorly.api.objects.dto.UpdateDeletionStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/stored-objects", "/v1/stored-objects"})
@RequiredArgsConstructor
public class StoredObjectController {
  private final StoredObjectService storedObjectService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public StoredObjectResponse create(@Valid @RequestBody CreateStoredObjectRequest request) {
    return storedObjectService.create(request);
  }

  @GetMapping("/{id}")
  public StoredObjectResponse findById(@PathVariable UUID id) {
    return storedObjectService.findById(id);
  }

  @PatchMapping("/{id}/deletion-status")
  public StoredObjectResponse updateDeletionStatus(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateDeletionStatusRequest request
  ) {
    return storedObjectService.updateDeletionStatus(id, request.deletionStatus());
  }

  @GetMapping("/owner/{ownerId}")
  public List<StoredObjectResponse> findAllByOwnerId(@PathVariable UUID ownerId) {
    return storedObjectService.findAllByOwnerId(ownerId);
  }
}
