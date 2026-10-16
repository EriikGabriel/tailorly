import { defineConfig } from "orval";

export default defineConfig({
  tailorly: {
    input: {
      target: "../../services/api/openapi/tailorly.json",
    },
    output: {
      target: "./src/lib/api/generated/tailorly.ts",
      schemas: "./src/lib/api/generated/model",
      client: "fetch",
      mode: "single",
      clean: true,
      urlEncodeParameters: true,
      override: {
        mutator: {
          path: "./src/lib/api/client.ts",
          name: "apiRequest",
        },
        fetch: {
          includeHttpResponseReturnType: false,
        },
      },
    },
  },
});
