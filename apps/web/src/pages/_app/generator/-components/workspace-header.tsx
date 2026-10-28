import {
  House,
  Link as LinkIcon,
  Network,
  Sparkles,
} from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";

export function WorkspaceHeader({ baseCvFile }: { baseCvFile: File | null }) {
  const statusLabel = baseCvFile
    ? "Ground Truth configurado"
    : "Aguardando Currículo Base";

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3 pb-1">
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
            <li aria-current="page" className="font-semibold text-primary-950">
              Gerador de Currículos
            </li>
          </ol>
        </nav>
        <span className="inline-flex items-center gap-2 rounded-full bg-surface-container px-4 py-1 text-xs font-semibold leading-4 tracking-tight text-primary-950 shadow-xs">
          <span
            aria-hidden="true"
            className="size-2.5 rounded-full bg-on-tertiary-container"
          />
          {statusLabel}
        </span>
      </div>

      <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex max-w-2xl flex-col gap-1 xl:max-w-[46%]">
          <p className="flex items-center gap-1 text-xs font-semibold leading-4 tracking-[0.6px] text-muted-foreground uppercase">
            <Sparkles
              aria-hidden="true"
              className="size-5 shrink-0 text-primary-950"
            />
            Geração assistida por IA
          </p>
          <h1
            id="generator-heading"
            className="text-headline-lg-mobile font-bold tracking-tight sm:text-headline-lg sm:font-bold sm:tracking-[-0.8px]"
          >
            Gerador de Currículo Sob Medida
          </h1>
          <p className="text-body-md text-on-surface-variant">
            Alimentado por IA e integrado ao seu Second Brain (Notion/Obsidian)
            para extrair e sintetizar suas experiências de maior impacto para{" "}
            <strong className="font-semibold text-primary-950">
              cada vaga específica
            </strong>
            .
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap xl:max-w-[52%] xl:justify-end">
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-outline-variant/60 bg-card px-3.5 py-2.5 shadow-xs">
            <span className="inline-flex items-center gap-1.5 pr-2">
              <LinkIcon
                aria-hidden="true"
                className="size-3.5 text-on-surface-variant"
              />
              <span className="text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface">
                Fonte Principal (Ground Truth):
              </span>
              <span className="rounded bg-secondary-container px-2 py-0.5 text-xs font-semibold leading-4 tracking-[0.3px] text-on-secondary-container">
                {baseCvFile ? baseCvFile.name : "Nenhum arquivo"}
                {baseCvFile && " (Ativo)"}
              </span>
            </span>
            <span aria-hidden="true" className="text-outline-variant">
              •
            </span>
            <span className="inline-flex items-center gap-1.5">
              <Network
                aria-hidden="true"
                className="size-3.5 text-on-surface-variant"
              />
              <span className="text-xs font-semibold leading-4 tracking-[0.3px] text-on-surface-variant">
                Second Brain (Desconectado - Opcional)
              </span>
            </span>
          </div>
        </div>
      </div>
    </>
  );
}
