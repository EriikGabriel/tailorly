// Testes unitários (node:test) de stores e utilitários. Compila tests/unit e o que ele importa com o
// tsc para um diretório temporário e resolve os aliases do tsconfig.app.json em runtime.
import { spawnSync } from "node:child_process";
import {
  mkdtempSync,
  readdirSync,
  readFileSync,
  rmSync,
  symlinkSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("..", import.meta.url));
const output = mkdtempSync(path.join(tmpdir(), "tailorly-store-tests-"));
const tests = readdirSync(path.join(root, "tests/unit"))
  .filter((file) => file.endsWith(".test.ts"))
  .sort()
  .map((file) => `tests/unit/${file}`);

const { paths } = JSON.parse(
  readFileSync(path.join(root, "tsconfig.app.json"), "utf8"),
).compilerOptions;
// "@stores/*": ["./src/stores/*"] -> { "@stores": "src/stores" }
const aliases = Object.fromEntries(
  Object.entries(paths)
    .filter(([key]) => key.startsWith("@"))
    .map(([key, [target]]) => [
      key.replace(/\/\*$/, ""),
      target.replace(/^\.\//, "").replace(/\/\*$/, ""),
    ]),
);

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
  // Resolve "@alias/x" para o JS compilado, na mesma estrutura de src/.
  writeFileSync(
    path.join(output, "alias-hook.cjs"),
    `const Module = require("node:module");
const path = require("node:path");
const aliases = ${JSON.stringify(aliases)};
const prefixes = Object.keys(aliases).sort((a, b) => b.length - a.length);
const resolve = Module._resolveFilename;
Module._resolveFilename = function (request, ...rest) {
  const prefix = prefixes.find((key) => request.startsWith(key + "/"));
  if (prefix) {
    request = path.join(__dirname, aliases[prefix], request.slice(prefix.length));
  }
  return resolve.call(this, request, ...rest);
};
`,
  );
  writeFileSync(
    path.join(output, "tsconfig.json"),
    JSON.stringify({
      compilerOptions: {
        module: "commonjs",
        moduleResolution: "node",
        target: "ES2022",
        esModuleInterop: true,
        skipLibCheck: true,
        strict: true,
        jsx: "react-jsx",
        baseUrl: root,
        paths,
        outDir: output,
        rootDir: root,
      },
      files: tests.map((file) => path.join(root, file)),
    }),
  );
  const compiled = spawnSync(
    process.execPath,
    [
      path.join(root, "node_modules/typescript/bin/tsc"),
      "-p",
      path.join(output, "tsconfig.json"),
    ],
    { cwd: root, stdio: "inherit" },
  );
  if (compiled.status !== 0) process.exitCode = compiled.status ?? 1;
  else {
    const result = spawnSync(
      process.execPath,
      [
        "--require",
        path.join(output, "alias-hook.cjs"),
        "--test",
        ...tests.map((file) => path.join(output, file.replace(/\.ts$/, ".js"))),
      ],
      { cwd: root, stdio: "inherit" },
    );
    process.exitCode = result.status ?? 1;
  }
} finally {
  rmSync(output, { recursive: true, force: true });
}
