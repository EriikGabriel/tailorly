import { Button } from "@animate/buttons/button";
import { AutoHeight } from "@animate/primitives/effects/auto-height";
import {
  CircleCheck,
  FileText,
  FileUp,
  House,
  ListChecks,
  Shield,
  ShieldCheck,
} from "@react-zero-ui/icon-sprite";
import { useBaseCvStore } from "@stores/base-cv-store";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useReducedMotion } from "motion/react";
import { useRef } from "react";
import { GovernanceCard } from "./-components/governance-card";
import { ReviewWorkspace } from "./-components/review-workspace";

export const Route = createFileRoute("/_app/base/")({
  component: RouteComponent,
});

function RouteComponent() {
  const inputRef = useRef<HTMLInputElement>(null);
  const file = useBaseCvStore((state) => state.file);
  const error = useBaseCvStore((state) => state.error);
  const selectFile = useBaseCvStore((state) => state.selectFile);
  const reducedMotion = useReducedMotion();

  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <section
        aria-labelledby="base-heading"
        className="mx-auto flex w-full max-w-384 flex-col gap-4 rounded-2xl  p-4 shadow-xs sm:p-6"
      >
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
              <li>Currículo Base</li>
              <li aria-hidden="true" className="text-outline-variant">
                /
              </li>
              <li
                aria-current="page"
                className="font-semibold text-primary-950"
              >
                Auditoria &amp; Curadoria do Ground Truth
              </li>
            </ol>
          </nav>
          <span className="inline-flex items-center gap-2 rounded-full bg-surface-container px-4 py-1 text-xs font-semibold leading-4 tracking-tight text-primary-950 shadow-xs">
            <span
              aria-hidden="true"
              className="size-2.5 rounded-full bg-on-tertiary-container"
            />
            Aguardando Currículo Base
          </span>
        </div>
        <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex max-w-2xl flex-col gap-1 xl:max-w-[46%]">
            <p className="flex items-center gap-1 text-xs font-semibold leading-4 tracking-[0.6px] text-muted-foreground uppercase">
              <ShieldCheck
                aria-hidden="true"
                className="size-5 shrink-0 text-primary-950"
              />
              Cofre de conhecimento imutável
            </p>
            <h1
              id="base-heading"
              className="text-headline-lg-mobile font-bold tracking-tight sm:text-headline-lg sm:font-bold sm:tracking-[-0.8px]"
            >
              Auditoria &amp; Curadoria dos Fatos do Currículo Base
            </h1>
            <p className="text-body-md text-on-surface-variant">
              Validação estruturada dos dados extraídos do seu currículo base.
              Revise fatos, credenciais e métricas para que ele seja a fonte das
              próximas adaptações, com{" "}
              <strong className="font-semibold text-primary-950">
                informações confirmadas por você
              </strong>
              .
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap xl:max-w-[52%] xl:justify-end">
            <input
              ref={inputRef}
              type="file"
              accept=".pdf,application/pdf"
              aria-label="Selecionar PDF do Currículo Base"
              className="hidden"
              onChange={(event) => {
                const selected = event.target.files?.[0];
                event.target.value = "";
                if (!selected) return;
                selectFile(selected, "pdf");
              }}
            />
            <Button
              variant="accent"
              onClick={() => inputRef.current?.click()}
              className="h-auto min-h-12 gap-1 rounded-xl bg-card px-6 py-3.5 text-label-md font-semibold text-primary-950 hover:bg-surface-container-high has-[>svg]:px-6"
            >
              <FileUp aria-hidden="true" className="size-4" />
              {file
                ? "Substituir PDF / Re-upload"
                : "Selecionar Currículo Base"}
            </Button>
            <Button
              disabled
              aria-describedby="base-availability"
              className="h-auto min-h-12 gap-1 whitespace-normal text-white disabled:text-white rounded-xl px-6 py-3.5 text-label-md font-semibold shadow-md has-[>svg]:px-6"
            >
              <CircleCheck aria-hidden="true" className="size-4" />
              Aprovar Ground Truth e Voltar ao Início ✓
            </Button>
          </div>
        </div>
        <AutoHeight
          deps={[file, error]}
          transition={reducedMotion ? { duration: 0 } : undefined}
        >
          <div className="space-y-4 pt-1">
            <div className="grid gap-4 md:grid-cols-3">
              <GovernanceCard
                label="Arquivo mestre"
                icon={<FileText aria-hidden="true" className="size-5" />}
              >
                <p
                  className="truncate text-title-md text-primary-950"
                  title={file?.name}
                >
                  {file?.name ?? "Nenhum arquivo selecionado"}
                </p>
                <p className="text-body-sm">
                  {file
                    ? `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(file.size / 1024)} KB • Prévia local, sem envio`
                    : "Selecione seu currículo em PDF"}
                </p>
              </GovernanceCard>
              <GovernanceCard
                label="Índice de confiabilidade"
                accent
                icon={<ListChecks aria-hidden="true" className="size-5" />}
              >
                <p className="flex flex-wrap items-baseline gap-1 pb-1">
                  <span className="text-xl font-bold leading-7 text-primary-950">
                    —
                  </span>
                  <span className="text-body-sm font-medium text-muted-foreground">
                    Ainda não avaliado
                  </span>
                </p>
                <div
                  aria-hidden="true"
                  className="h-1.5 rounded-full bg-surface-container-highest"
                />
              </GovernanceCard>
              <GovernanceCard
                label="Protocolo de integridade"
                icon={<Shield aria-hidden="true" className="size-5" />}
              >
                <p className="text-title-md text-primary-950">
                  Aguardando revisão
                </p>
                <p className="text-body-sm">Nenhum fato confirmado</p>
              </GovernanceCard>
            </div>

            <div aria-live="polite" className="sr-only">
              {file
                ? `${file.name} selecionado para prévia local.`
                : "Nenhum arquivo selecionado."}
            </div>
            {error && (
              <p role="alert" className="text-body-sm text-error">
                {error}
              </p>
            )}
          </div>
        </AutoHeight>
      </section>
      <ReviewWorkspace onSelectFile={() => inputRef.current?.click()} />
    </main>
  );
}
