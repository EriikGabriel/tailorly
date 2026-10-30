import { House } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

/** Caminho de navegação da página (Início / Currículo Base / Auditoria). */
export function BaseBreadcrumb() {
  return (
    <nav aria-label="Caminho de navegação">
      <ol className="flex flex-wrap items-center gap-x-1 gap-y-2 text-label-md text-on-surface-variant">
        <li>
          <Link
            to="/"
            className="inline-flex items-center gap-1 rounded-sm hover:text-primary focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring"
          >
            <House aria-hidden="true" className="size-3.5" />
            Início
          </Link>
        </li>
        <li aria-hidden="true" className="text-outline-variant">
          /
        </li>
        <li>Currículo Base</li>
        <li aria-hidden="true" className="text-outline-variant">
          /
        </li>
        <li aria-current="page" className="font-semibold text-primary-950">
          Auditoria &amp; Curadoria do Ground Truth
        </li>
      </ol>
    </nav>
  );
}
