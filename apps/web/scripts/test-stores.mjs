import { spawnSync } from "node:child_process";
import { mkdtempSync, rmSync, symlinkSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));
const output = mkdtempSync(path.join(tmpdir(), "tailorly-store-tests-"));
try {
  writeFileSync(
    path.join(output, "package.json"),
    JSON.stringify({ type: "commonjs" }),
  );
  symlinkSync(
    path.join(root, "node_modules"),
    path.join(output, "node_modules"),
    "dir",
  );
  const compiled = spawnSync(
    process.execPath,
    [
      path.join(root, "node_modules/typescript/bin/tsc"),
      "--module",
      "commonjs",
      "--moduleResolution",
      "node",
      "--target",
      "ES2022",
      "--esModuleInterop",
      "--skipLibCheck",
      "--strict",
      "--outDir",
      output,
      "tests/client-state.test.ts",
    ],
    { cwd: root, stdio: "inherit" },
  );
  if (compiled.status !== 0) process.exitCode = compiled.status ?? 1;
  else {
    const result = spawnSync(
      process.execPath,
      [path.join(output, "tests/client-state.test.js")],
      { cwd: root, stdio: "inherit" },
    );
    process.exitCode = result.status ?? 1;
  }
} finally {
  rmSync(output, { recursive: true, force: true });
}
