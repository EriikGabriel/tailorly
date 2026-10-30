package com.tailorly.api.shared.exception;

public final class ErrorCodes {
  public static final String HTTP_PREFIX = "error.http.";
  public static final String UNEXPECTED = "error.unexpected";

  private ErrorCodes() {
  }

  public static final class User {
    public static final String NOT_FOUND = "error.user.not_found";
    public static final String EMAIL_ALREADY_REGISTERED = "error.user.email_already_registered";
    public static final String ACCESS_DENIED = "error.user.access_denied";

    private User() {
    }
  }

  public static final class Owner {
    public static final String NOT_FOUND = "error.owner.not_found";
    public static final String USER_ONLY_FOR_ACCOUNT = "error.owner.user_only_for_account";
    public static final String ACCOUNT_AUTH_REQUIRED = "error.owner.account_auth_required";
    public static final String ACCOUNT_MISMATCH = "error.owner.account_mismatch";
    public static final String ACCESS_DENIED = "error.owner.access_denied";
    public static final String INVALID_KIND = "error.owner.invalid_kind";
    public static final String NO_ACTIVE_SNAPSHOT = "error.owner.no_active_snapshot";

    private Owner() {
    }
  }

  public static final class Session {
    public static final String NOT_FOUND = "error.session.not_found";
    public static final String INVALID_TOKEN = "error.session.invalid_token";
    public static final String RATE_LIMITED = "error.session.rate_limited";
    public static final String CAPACITY_REACHED = "error.session.capacity_reached";
    public static final String SESSION_REQUIRED = "error.session.session_required";

    private Session() {
    }
  }

  public static final class Anonymous {
    public static final String FILE_TOO_LARGE = "error.anonymous.file_too_large";
    public static final String PAGE_LIMIT_EXCEEDED = "error.anonymous.page_limit_exceeded";
    public static final String UPLOAD_LIMIT_REACHED = "error.anonymous.upload_limit_reached";
    public static final String EXTRACTION_LIMIT_REACHED = "error.anonymous.extraction_limit_reached";
    public static final String GENERATION_LIMIT_REACHED = "error.anonymous.generation_limit_reached";
    public static final String JOB_LIMIT_REACHED = "error.anonymous.job_limit_reached";
    public static final String NOTHING_TO_MIGRATE = "error.anonymous.nothing_to_migrate";
    public static final String ACTIVE_SNAPSHOT_EXISTS = "error.anonymous.active_snapshot_exists";
    public static final String ANONYMOUS_OWNER_CREATION_FORBIDDEN =
        "error.anonymous.owner_creation_forbidden";

    private Anonymous() {
    }
  }

  public static final class StoredObject {
    public static final String NOT_FOUND = "error.stored_object.not_found";
    public static final String STORAGE_KEY_ALREADY_EXISTS = "error.stored_object.storage_key_already_exists";
    public static final String OWNER_MISMATCH = "error.stored_object.owner_mismatch";
    public static final String INVALID_DELETION_STATUS = "error.stored_object.invalid_deletion_status";

    private StoredObject() {
    }
  }

  public static final class BaseDocument {
    public static final String NOT_FOUND = "error.base_document.not_found";
    public static final String CONTENT_ALREADY_EXISTS = "error.base_document.content_already_exists";
    public static final String HASH_NOT_UNIQUE = "error.base_document.hash_not_unique";
    public static final String ORIGINAL_OBJECT_OWNER_MISMATCH = "error.base_document.original_object_owner_mismatch";
    public static final String INVALID_PAGE_COUNT = "error.base_document.invalid_page_count";
    public static final String INVALID_PDF = "error.base_document.invalid_pdf";
    public static final String FILE_TOO_LARGE = "error.base_document.file_too_large";
    public static final String ORIGINAL_OBJECT_MISSING = "error.base_document.original_object_missing";

    private BaseDocument() {
    }
  }

  public static final class BaseVersion {
    public static final String NOT_FOUND = "error.base_version.not_found";
    public static final String VERSION_ALREADY_EXISTS = "error.base_version.version_already_exists";
    public static final String DOCUMENT_OWNER_MISMATCH = "error.base_version.document_owner_mismatch";
    public static final String INVALID_STATUS = "error.base_version.invalid_status";
    public static final String REVIEW_PENDING = "error.base_version.review_pending";
    public static final String NO_APPROVED_FACTS = "error.base_version.no_approved_facts";
    public static final String DECISION_MISSING = "error.base_version.decision_missing";

    private BaseVersion() {
    }
  }

  public static final class GtSnapshot {
    public static final String NOT_FOUND = "error.gt_snapshot.not_found";
    public static final String NOT_ACTIVE = "error.gt_snapshot.not_active";

    private GtSnapshot() {
    }
  }

  public static final class ExtractionRun {
    public static final String NOT_FOUND = "error.extraction_run.not_found";
    public static final String BASE_VERSION_OWNER_MISMATCH = "error.extraction_run.base_version_owner_mismatch";
    public static final String OUTPUT_OBJECT_OWNER_MISMATCH = "error.extraction_run.output_object_owner_mismatch";
    public static final String INVALID_STATUS = "error.extraction_run.invalid_status";
    public static final String ALREADY_FINISHED = "error.extraction_run.already_finished";

    private ExtractionRun() {
    }
  }

  public static final class ExtractedPage {
    public static final String NOT_FOUND = "error.extracted_page.not_found";
    public static final String TEXT_OBJECT_OWNER_MISMATCH = "error.extracted_page.text_object_owner_mismatch";
    public static final String DUPLICATE_PAGE_NUMBER = "error.extracted_page.duplicate_page_number";
    public static final String HASH_NOT_UNIQUE = "error.extracted_page.hash_not_unique";
    public static final String INVALID_PAGE_NUMBER = "error.extracted_page.invalid_page_number";

    private ExtractedPage() {
    }
  }

  public static final class FactRecord {
    public static final String NOT_FOUND = "error.fact_record.not_found";
    public static final String INVALID_DISPLAY_ORDER = "error.fact_record.invalid_display_order";

    private FactRecord() {
    }
  }

  public static final class Fact {
    public static final String NOT_FOUND = "error.fact.not_found";
    public static final String RECORD_VERSION_MISMATCH = "error.fact.record_version_mismatch";
    public static final String EXTRACTION_VERSION_MISMATCH = "error.fact.extraction_version_mismatch";
    public static final String INVALID_REVIEW_STATE = "error.fact.invalid_review_state";
    public static final String INVALID_CONFIRMATION = "error.fact.invalid_confirmation";
    public static final String INVALID_CORRECTION = "error.fact.invalid_correction";

    private Fact() {
    }
  }

  public static final class FactEvidence {
    public static final String NOT_FOUND = "error.fact_evidence.not_found";
    public static final String PAGE_VERSION_MISMATCH = "error.fact_evidence.page_version_mismatch";
    public static final String EXCERPT_NOT_IN_SOURCE = "error.fact_evidence.excerpt_not_in_source";

    private FactEvidence() {
    }
  }

  public static final class Job {
    public static final String URL_INVALID = "error.job.url_invalid";
    public static final String URL_NOT_ALLOWED = "error.job.url_not_allowed";
    public static final String PAGE_UNAVAILABLE = "error.job.page_unavailable";
    public static final String NOT_HTML = "error.job.not_html";
    public static final String NO_CONTENT = "error.job.no_content";
    public static final String NOT_FOUND = "error.job.not_found";
    public static final String INPUT_REQUIRED = "error.job.input_required";

    private Job() {
    }
  }

  public static final class CvGeneration {
    public static final String NOT_FOUND = "error.cv_generation.not_found";
    public static final String NO_ACTIVE_SNAPSHOT = "error.cv_generation.no_active_snapshot";
    public static final String JOB_NOT_FOUND = "error.cv_generation.job_not_found";
    public static final String TEMPLATE_UNKNOWN = "error.cv_generation.template_unknown";
    public static final String INVALID_FORMAT = "error.cv_generation.invalid_format";
    public static final String ARTIFACT_MISSING = "error.cv_generation.artifact_missing";
    public static final String RENDERER_UNAVAILABLE = "error.cv_generation.renderer_unavailable";
    public static final String TEMPLATE_INVALID = "error.cv_generation.template_invalid";
    public static final String TEMPLATE_NOT_FOUND = "error.cv_generation.template_not_found";
    public static final String TEMPLATE_LIMIT_REACHED = "error.cv_generation.template_limit_reached";
    public static final String PREFERENCES_INVALID = "error.cv_generation.preferences_invalid";

    private CvGeneration() {
    }
  }

  public static final class RefreshToken {
    public static final String NOT_FOUND = "error.refresh_token.not_found";

    private RefreshToken() {
    }
  }

  public static final class Security {
    public static final String AUTHENTICATION_REQUIRED = "error.security.authentication_required";
    public static final String ACCESS_DENIED = "error.security.access_denied";

    private Security() {
    }
  }

  public static final class Validation {
    public static final String FAILED = "error.validation.failed";
    public static final String REQUEST = "error.validation.request";

    private Validation() {
    }
  }

  public static final class Request {
    public static final String MALFORMED = "error.request.malformed";

    private Request() {
    }
  }
}
