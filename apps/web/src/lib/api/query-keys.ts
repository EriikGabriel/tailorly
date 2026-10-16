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
} as const;
