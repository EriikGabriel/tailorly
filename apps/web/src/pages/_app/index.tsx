import { HeroIngestionCard } from "@components/hero-ingestion-card";
import { ShieldCheckIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { createFileRoute } from "@tanstack/react-router";
import { Badge } from "@ui/badge";

export const Route = createFileRoute("/_app/")({
  component: App,
});

function App() {
  return (
    <main className="relative isolate flex min-h-[calc(100dvh-4rem)] flex-col items-center overflow-hidden px-4 sm:px-8 lg:px-16">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-32 left-1/2 h-85 w-210 -translate-x-1/2 bg-linear-to-b from-secondary-container/25 via-surface-container-low/40 to-surface-container-low/0 blur-[32px]"
      />

      <section className="relative z-10 flex w-full max-w-3/5 flex-col items-center justify-center gap-6 p-3">
        <Badge
          variant="outline"
          className="mt-8 h-auto overflow-visible border-primary-200 bg-card py-1.5 px-4 font-semibold text-primary-900 drop-shadow-lg"
        >
          <div className="relative mr-1 inline-flex size-3 shrink-0 items-center justify-center">
            <span
              aria-hidden="true"
              className="absolute size-2 animate-ping rounded-full bg-tertiary-600 opacity-75"
            />
            <span className="relative size-2 rounded-full bg-tertiary-600" />
          </div>
          Arquitetura Ground Truth
        </Badge>

        <div className="flex flex-col items-center gap-6 text-center">
          <h1 className="relative text-4xl font-bold leading-tight tracking-[-1.4px] text-primary-950 md:text-5xl xl:text-6xl xl:leading-16 ">
            Seu currículo perfeito para cada <br className="hidden" />
            vaga em segundos.
          </h1>
          <p className="w-full max-w-3/5 text-secondary-700">
            A Tailorly usa o seu Currículo Base salvo como verdade absoluta
            (Ground Truth), analisa os requisitos da vaga e gera uma versão sob
            medida com foco cirúrgico em ATS e recrutadores.
          </p>
        </div>

        <div className="flex flex-col w-full gap-3">
          <HeroIngestionCard />

          <small className="flex gap-3 justify-center items-center font-semibold">
            <HugeiconsIcon
              icon={ShieldCheckIcon}
              className="size-4.5 shrink-0 text-tertiary"
            />
            Utiliza seus dados reais comprovados. Sem inventar fatos. Second
            Brain opcional para notas complementares.
          </small>
        </div>
      </section>
    </main>
  );
}
