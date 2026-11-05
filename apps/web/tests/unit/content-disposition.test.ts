import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { filenameFromDisposition } from "@api/client";

describe("filenameFromDisposition", () => {
  it("reads a quoted filename", () => {
    assert.equal(
      filenameFromDisposition(
        'attachment; filename="erik_dev-backend_acme.pdf"',
      ),
      "erik_dev-backend_acme.pdf",
    );
  });

  it("prefers the UTF-8 encoded filename", () => {
    assert.equal(
      filenameFromDisposition(
        "attachment; filename=\"fallback.pdf\"; filename*=UTF-8''jos%C3%A9.pdf",
      ),
      "josé.pdf",
    );
  });

  it("returns null when there is no header or filename", () => {
    assert.equal(filenameFromDisposition(null), null);
    assert.equal(filenameFromDisposition("attachment"), null);
  });
});
