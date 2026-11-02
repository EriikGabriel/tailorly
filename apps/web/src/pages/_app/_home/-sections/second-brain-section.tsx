import { useSecondBrain } from "@queries/second-brain";
import {
  Archive,
  BookOpen,
  CircleCheck,
  CircleDashed,
  Globe,
  type IconProps,
  Network,
} from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import type { ComponentType } from "react";

interface Capability {
  label: string;
  description: string;
  active: boolean;
  icon: ComponentType<IconProps>;
}

/**
 * Integração opcional com o Second Brain (Obsidian). Mostra o que a conta tem de fato conectado e o que
 * cada recurso faz; não é uma propaganda estática. Nada aqui muda o Currículo Base.
 */
export function SecondBrainSection() {
  const { isAccount, isLoading, status } = useSecondBrain();

  const capabilities: Capability[] = [
    {
      label: "Leitura de notas",
      description:
        "Busca projetos e detalhes nas suas notas, como evidência complementar que você confirma antes de entrar no currículo.",
      active: status.readsNotes,
      icon: BookOpen,
    },
    {
      label: "Arquivo de currículos",
      description:
        "Copia o PDF e o Markdown de cada currículo gerado para uma pasta do seu vault.",
      active: status.exportsArtifacts,
      icon: Archive,
    },
    {
      label: "Busca web de projetos",
      description:
        "Procura páginas públicas dos seus projetos para complementar as notas.",
      active: status.webSearch,
      icon: Globe,
    },
  ];
  const activeCount = capabilities.filter((item) => item.active).length;

  const headline = !isAccount
    ? "Entre na sua conta para conectar o Second Brain"
    : status.connected
      ? activeCount > 0
        ? `Second Brain conectado · ${activeCount} de ${capabilities.length} recursos ativos`
        : "Second Brain conectado, mas sem recursos ativos"
      : "Conecte o Obsidian e enriqueça seus currículos";

  const intro = !isAccount
    ? "Visitantes usam o Tailorly só com o Currículo Base da sessão. Com uma conta, o Obsidian pode ser usado como fonte secundária."
    : status.connected
      ? "Cada recurso abaixo pode ser ligado ou desligado no seu perfil. O Currículo Base continua sendo a única fonte dos fatos."
      : "Opcional: o Tailorly funciona com o seu Currículo Base. Ao conectar o Obsidian, suas notas viram evidência complementar (sempre com sua confirmação) e seus currículos podem ser arquivados no vault.";

  return (
    <section
      aria-labelledby="second-brain-heading"
      className="flex w-full flex-col gap-6 overflow-hidden rounded-xl bg-primary-900 p-6 shadow-[0_10px_15px_-3px_rgba(0,0,0,0.1),0_4px_6px_-4px_rgba(0,0,0,0.1)] sm:p-10"
    >
      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex min-w-0 items-start gap-4 sm:gap-6">
          <span className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-white/10">
            <Network aria-hidden="true" className="size-8 text-[#ffdcc3]" />
          </span>
          <div className="min-w-0 space-y-1 pt-1">
            <span className="inline-flex rounded bg-white/15 px-2 py-0.5 text-xs font-semibold leading-4 tracking-[0.3px] text-[#ffdcc3]">
              Recurso opcional
            </span>
            <h2
              id="second-brain-heading"
              className="text-xl font-semibold leading-7 tracking-[-0.5px] text-white"
            >
              {isLoading ? "Verificando o Second Brain..." : headline}
            </h2>
            <p className="max-w-2xl text-body-md leading-6 text-outline-variant">
              {intro}
            </p>
          </div>
        </div>

        <div className="flex shrink-0 flex-col gap-2 sm:flex-row">
          {isAccount ? (
            <Link
              to="/profile"
              hash="section-connections"
              className="inline-flex min-h-10 items-center justify-center rounded-lg bg-white px-4 py-2.5 text-center text-sm font-semibold leading-5 tracking-[0.14px] text-primary-900 hover:bg-surface-container-low focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              {status.connected ? "Gerenciar conexão" : "Conectar Second Brain"}
            </Link>
          ) : (
            <Link
              to="/profile"
              className="inline-flex min-h-10 items-center justify-center rounded-lg bg-white px-4 py-2.5 text-center text-sm font-semibold leading-5 tracking-[0.14px] text-primary-900 hover:bg-surface-container-low focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
            >
              Criar conta
            </Link>
          )}
          <a
            href="#hero-ingestion"
            className="inline-flex min-h-10 items-center justify-center rounded-lg border border-outline-variant/30 px-4 py-2.5 text-center text-sm font-medium leading-5 tracking-[0.14px] text-surface hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white"
          >
            Continuar só com o Currículo Base
          </a>
        </div>
      </div>

      {isAccount && (
        <ul className="grid gap-3 md:grid-cols-3">
          {capabilities.map(({ label, description, active, icon: Icon }) => (
            <li key={label} className="flex gap-3 rounded-lg bg-white/10 p-4">
              <span
                className={`flex size-8 shrink-0 items-center justify-center rounded-full ${active ? "bg-[#ffdcc3] text-primary-900" : "bg-white/15 text-white"}`}
              >
                <Icon aria-hidden="true" className="size-4" />
              </span>
              <div className="min-w-0 space-y-1">
                <p className="flex items-center gap-2 text-sm font-semibold text-white">
                  {label}
                  <span className="inline-flex items-center gap-1 text-xs font-medium text-outline-variant">
                    {active ? (
                      <>
                        <CircleCheck aria-hidden="true" className="size-3" />
                        Ativo
                      </>
                    ) : (
                      <>
                        <CircleDashed aria-hidden="true" className="size-3" />
                        {status.connected ? "Desligado" : "Não conectado"}
                      </>
                    )}
                  </span>
                </p>
                <p className="text-xs leading-5 text-outline-variant">
                  {description}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
