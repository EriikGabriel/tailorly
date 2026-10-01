import { ShieldCheckIcon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import { Badge } from "@ui/badge";
import { HeroIngestionCard } from "../-components/hero-ingestion-card";

export function HeroSection() {
  return (
    <section
      id="hero-ingestion"
      className="relative z-10 grid w-full max-w-360 items-center gap-8 lg:grid-cols-[minmax(0,1.12fr)_minmax(0,1fr)] lg:gap-10 xl:gap-14 py-6 "
    >
      <div className="flex max-w-2xl flex-col items-start gap-5">
        <Badge
          variant="outline"
          className="h-auto overflow-visible border-primary-200 bg-card px-4 py-1.5 font-semibold text-primary-900 drop-shadow-lg"
        >
          <span className="relative mr-1 inline-flex size-3 shrink-0 items-center justify-center">
            <span
              aria-hidden="true"
              className="absolute size-2 animate-ping rounded-full bg-tertiary-600 opacity-75"
            />
            <span className="relative size-2 rounded-full bg-tertiary-600" />
          </span>
          Arquitetura Ground Truth
        </Badge>

        <h1 className="text-4xl font-bold leading-tight tracking-[-1.4px] text-primary-950 md:text-5xl xl:text-6xl xl:leading-[1.08]">
          Seu currículo perfeito para cada vaga.
        </h1>
        <p className="max-w-xl text-base leading-7 text-secondary-700 lg:text-lg lg:leading-8">
          A Tailorly usa o seu Currículo Base como fonte principal, analisa os
          requisitos da vaga e cria uma versão sob medida para ATS e
          recrutadores.
        </p>
        <div className="flex max-w-xl items-start gap-3 border-l-2 border-tertiary-600 pl-4 text-sm leading-6 text-secondary-700">
          <HugeiconsIcon
            icon={ShieldCheckIcon}
            className="mt-1 size-4.5 shrink-0 text-tertiary"
          />
          <p>
            Seus dados reais guiam cada adaptação. Sem inventar fatos. Second
            Brain opcional para notas complementares.
          </p>
        </div>
      </div>

      <div className="min-w-0 w-full">
        <HeroIngestionCard />
      </div>
    </section>
  );
}
