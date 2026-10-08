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
