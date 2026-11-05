import assert from "node:assert/strict";
import { beforeEach, describe, it } from "node:test";
import { resetClientState } from "@stores/reset-client-state";
import {
  TEMPLATE_DRAFT_KEY,
  useTemplateDraftStore,
} from "@stores/template-draft-store";

describe("template draft store", () => {
  beforeEach(resetClientState);

  it("keeps the unsaved Typst edit for the open generation", () => {
    useTemplateDraftStore.getState().setDraft("gen-1", "#set text(12pt)");
    const state = useTemplateDraftStore.getState();
    assert.equal(state.generationId, "gen-1");
    assert.equal(state.content, "#set text(12pt)");
  });

  it("remembers the saved template without touching the draft", () => {
    const store = useTemplateDraftStore.getState();
    store.setDraft("gen-1", "edited");
    store.markSaved("gen-1", "edited");
    const state = useTemplateDraftStore.getState();
    assert.equal(state.savedContent, "edited");
    assert.equal(state.content, "edited");
  });

  it("drops the saved marker when another generation is edited", () => {
    const store = useTemplateDraftStore.getState();
    store.markSaved("gen-1", "old");
    store.setDraft("gen-2", "new");
    assert.equal(useTemplateDraftStore.getState().savedContent, null);
  });

  it("is cleared when the client state is reset", () => {
    useTemplateDraftStore.getState().setDraft("gen-1", "edited");
    resetClientState();
    const state = useTemplateDraftStore.getState();
    assert.equal(state.content, null);
    assert.equal(state.generationId, null);
  });

  it("mirrors the draft into sessionStorage so a reload keeps it", () => {
    const memory = new Map<string, string>();
    const original = Object.getOwnPropertyDescriptor(
      globalThis,
      "sessionStorage",
    );
    Object.defineProperty(globalThis, "sessionStorage", {
      configurable: true,
      value: {
        getItem: (key: string) => memory.get(key) ?? null,
        setItem: (key: string, value: string) => memory.set(key, value),
        removeItem: (key: string) => memory.delete(key),
      },
    });
    try {
      useTemplateDraftStore.getState().setDraft("gen-1", "edited");
      assert.deepEqual(JSON.parse(memory.get(TEMPLATE_DRAFT_KEY) ?? "null"), {
        generationId: "gen-1",
        content: "edited",
        savedContent: null,
      });
      resetClientState();
      assert.equal(memory.has(TEMPLATE_DRAFT_KEY), false);
    } finally {
      if (original)
        Object.defineProperty(globalThis, "sessionStorage", original);
      else Reflect.deleteProperty(globalThis, "sessionStorage");
    }
  });
});
