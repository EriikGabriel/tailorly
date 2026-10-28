export const apiQueryKeys = {
  authentication: {
    login: ["authentication", "login"] as const,
    register: ["authentication", "register"] as const,
  },
  account: {
    current: ["account", "current"] as const,
  },
  baseDocuments: {
    all: ["base-documents"] as const,
    byOwner: (ownerId: string) =>
      [...apiQueryKeys.baseDocuments.all, "owner", ownerId] as const,
    detail: (documentId: string) =>
      [...apiQueryKeys.baseDocuments.all, "detail", documentId] as const,
  },
  extractionRuns: {
    byVersion: (versionId: string) =>
      ["extraction-runs", "version", versionId] as const,
  },
  baseReview: {
    records: (versionId: string) =>
      ["base-review", versionId, "records"] as const,
    issues: (versionId: string) =>
      ["base-review", versionId, "issues"] as const,
    pages: (runId: string) =>
      ["base-review", "extracted-pages", runId] as const,
  },
  anonymousSession: {
    current: ["anonymous-session", "current"] as const,
  },
  aiPreferences: {
    current: ["ai-preferences", "current"] as const,
  },
} as const;
