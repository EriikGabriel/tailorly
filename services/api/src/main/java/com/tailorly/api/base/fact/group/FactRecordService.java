package com.tailorly.api.base.fact.group;

import java.util.List;
import java.util.UUID;

import com.tailorly.api.base.fact.group.dto.CreateFactRecordRequest;
import com.tailorly.api.base.fact.group.dto.FactRecordResponse;
import com.tailorly.api.base.fact.group.dto.UpdateDisplayOrderRequest;
import com.tailorly.api.base.fact.FactRepository;
import com.tailorly.api.base.fact.dto.FactResponse;
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
  private final FactRepository facts;
  private final BaseVersionRepository versions;
  private final OwnerAccessService access;

  @Transactional
  public FactRecordResponse create(CreateFactRecordRequest request) {
    BaseVersion version = getVersion(request.baseVersionId());
    access.assertCanAccess(version.getOwner());
    FactRecord record = records.save(FactRecord.builder()
        .baseVersion(version)
        .category(request.category().trim())
        .displayOrder(request.displayOrder())
        .build());
    return response(record);
  }

  @Transactional(readOnly = true)
  public FactRecordResponse findById(UUID id) {
    FactRecord factRecord = findFactRecord(id);
    access.assertCanAccess(factRecord.getBaseVersion().getOwner());
    return response(factRecord);
  }

  @Transactional(readOnly = true)
  public List<FactRecordResponse> findByVersion(UUID versionId) {
    BaseVersion version = getVersion(versionId);
    access.assertCanAccess(version.getOwner());
    return records.findAllByBaseVersionIdOrderByDisplayOrderAsc(versionId).stream()
        .map(this::response).toList();
  }

  @Transactional
  public FactRecordResponse updateDisplayOrder(UUID id, UpdateDisplayOrderRequest request) {
    FactRecord factRecord = findFactRecord(id);
    access.assertCanAccess(factRecord.getBaseVersion().getOwner());
    factRecord.setDisplayOrder(request.displayOrder());
    return response(records.save(factRecord));
  }

  private FactRecordResponse response(FactRecord record) {
    return FactRecordResponse.from(record, facts.findAllByFactRecord_IdOrderByFieldKeyAsc(record.getId())
        .stream().map(FactResponse::from).toList());
  }

  private FactRecord findFactRecord(UUID id) {
    return records.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.FactRecord.NOT_FOUND));
  }

  private BaseVersion getVersion(UUID id) {
    return versions.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BaseVersion.NOT_FOUND));
  }
}
