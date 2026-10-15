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

    private Owner() {
    }
  }

  public static final class Session {
    public static final String NOT_FOUND = "error.session.not_found";
    public static final String OWNER_MUST_BE_ANONYMOUS = "error.session.owner_must_be_anonymous";
    public static final String EXPIRATION_MUST_BE_FUTURE = "error.session.expiration_must_be_future";
    public static final String OWNER_ALREADY_HAS_SESSION = "error.session.owner_already_has_session";
    public static final String TOKEN_ALREADY_EXISTS = "error.session.token_already_exists";

    private Session() {
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

    private BaseDocument() {
    }
  }

  public static final class BaseVersion {
    public static final String NOT_FOUND = "error.base_version.not_found";
    public static final String VERSION_ALREADY_EXISTS = "error.base_version.version_already_exists";
    public static final String DOCUMENT_OWNER_MISMATCH = "error.base_version.document_owner_mismatch";
    public static final String INVALID_STATUS = "error.base_version.invalid_status";

    private BaseVersion() {
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
