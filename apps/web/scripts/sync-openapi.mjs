import { mkdir, rename, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const webRoot = fileURLToPath(new URL("..", import.meta.url));
const target = path.resolve(
  webRoot,
  "../../services/api/openapi/tailorly.json",
);
const temporary = `${target}.tmp`;
const source =
  process.env.TAILORLY_OPENAPI_URL ?? "http://localhost:8080/v3/api-docs";

const response = await fetch(source, {
  headers: { Accept: "application/json" },
});
if (!response.ok) {
  throw new Error(`Não foi possível obter ${source}: HTTP ${response.status}`);
}

const document = await response.json();
if (
  typeof document !== "object" ||
  document === null ||
  typeof document.openapi !== "string" ||
  !document.openapi.startsWith("3.")
) {
  throw new Error(`${source} não retornou um documento OpenAPI 3 válido.`);
}

// A URL usada para exportar o contrato não deve ficar embutida no cliente.
delete document.servers;

await mkdir(path.dirname(target), { recursive: true });
await writeFile(temporary, `${JSON.stringify(document, null, 2)}\n`, "utf8");
await rename(temporary, target);
console.log(`OpenAPI atualizado em ${path.relative(webRoot, target)}`);
