import { Button } from "@animate/buttons/button";
import { Network } from "@react-zero-ui/icon-sprite";

export function SecondBrainSection() {
  return (
    <section
      aria-labelledby="second-brain-heading"
      className="flex w-full  flex-col gap-6 overflow-hidden rounded-xl bg-primary-900 p-6 shadow-[0_10px_15px_-3px_rgba(0,0,0,0.1),0_4px_6px_-4px_rgba(0,0,0,0.1)] sm:p-10 xl:flex-row xl:items-center xl:justify-between"
    >
      <div className="flex min-w-0 items-start gap-4 sm:items-center sm:gap-6">
        <span className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-white/10 backdrop-blur-[2px]">
          <Network className="size-8 text-[#ffdcc3]" />
        </span>
        <div className="min-w-0 max-w-3/4 space-y-1 pt-1">
          <span className="inline-flex rounded bg-white/15 px-2 py-0.5 text-xs font-semibold leading-4 tracking-[0.3px] text-[#ffdcc3]">
            Recurso Extra &amp; Opcional
          </span>
          <h2
            id="second-brain-heading"
            className="text-xl font-semibold leading-7 tracking-[-0.5px] text-white"
          >
            Recurso Extra &amp; Opcional: Integração com Second Brain
          </h2>
          <p className="text-body-md leading-6 text-outline-variant">
            O Tailorly funciona 100% com o seu Currículo Base salvo. Se desejar,
            conecte o Notion ou Obsidian como fonte de consulta secundária para
            enriquecer projetos detalhados e arquivar seus currículos gerados
            automaticamente.
          </p>
        </div>
      </div>

      <div className="flex shrink-0 flex-col gap-2 xl:ml-4">
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
          <Button
            type="button"
            className="min-h-10 rounded-lg bg-white px-4 py-2.5 text-center text-sm font-semibold leading-5 tracking-[0.14px] text-primary-900 hover:bg-surface-container-low focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          >
            Conectar Second Brain (Opcional)
          </Button>
          <a
            href="#hero-ingestion"
            className="inline-flex min-h-10 items-center justify-center rounded-lg border border-outline-variant/30 px-4 py-2.5 text-center text-sm font-medium leading-5 tracking-[0.14px] text-surface hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          >
            Continuar apenas com Currículo Base
          </a>
        </div>
      </div>
    </section>
  );
}
