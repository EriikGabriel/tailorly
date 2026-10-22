package com.tailorly.api.base.fact.record;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.record.dto.CreateFactRecordRequest;
import com.tailorly.api.base.fact.record.dto.FactRecordResponse;
import com.tailorly.api.base.fact.record.dto.UpdateDisplayOrderRequest;
import com.tailorly.api.base.version.BaseVersion;
import com.tailorly.api.base.version.BaseVersionRepository;
import com.tailorly.api.owner.OwnerAccessService;
import com.tailorly.api.shared.exception.ErrorCodes;
import com.tailorly.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FactRecordService {
  private final FactRecordRepository records;
  private final BaseVersionRepository versions;
  private final OwnerAccessService access;

  @Transactional
  public FactRecordResponse create(CreateFactRecordRequest request) {
    BaseVersion version = getVersion(request.baseVersionId());
    access.assertCanAccess(version.getOwner());
    return FactRecordResponse.from(records.save(FactRecord.builder()
        .baseVersion(version)
        .category(request.category().trim())
        .displayOrder(request.displayOrder())
        .build()));
  }

  @Transactional(readOnly = true)
  public FactRecordResponse findById(UUID id) {
    FactRecord record = getRecord(id);
    access.assertCanAccess(record.getBaseVersion().getOwner());
    return FactRecordResponse.from(record);
  }

  @Transactional(readOnly = true)
  public List<FactRecordResponse> findByVersion(UUID versionId) {
    BaseVersion version = getVersion(versionId);
    access.assertCanAccess(version.getOwner());
    return records.findAllByBaseVersionIdOrderByDisplayOrderAsc(versionId).stream()
        .map(FactRecordResponse::from).toList();
  }

  @Transactional
  public FactRecordResponse updateDisplayOrder(UUID id, UpdateDisplayOrderRequest request) {
    FactRecord record = getRecord(id);
    access.assertCanAccess(record.getBaseVersion().getOwner());
    record.setDisplayOrder(request.displayOrder());
    return FactRecordResponse.from(records.save(record));
  }

  private FactRecord getRecord(UUID id) {
    return records.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.FactRecord.NOT_FOUND));
  }

  private BaseVersion getVersion(UUID id) {
    return versions.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }
}
