import { House, type IconProps } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import type { ComponentType, ReactNode } from "react";

interface PageHeadingProps {
  /** `id` do `h1`, referenciado por `aria-labelledby` da seção da página. */
  headingId: string;
  breadcrumb: string;
  /** Selo no canto direito da linha do caminho de navegação. */
  status?: ReactNode;
  eyebrowIcon: ComponentType<IconProps>;
  eyebrow: string;
  title: ReactNode;
  description: ReactNode;
  /** Ações alinhadas à direita do título em telas largas. */
  actions?: ReactNode;
}

/** Cabeçalho padrão das páginas (mesmo layout e tipografia do Currículo Base e do Gerador). */
export function PageHeading({
  headingId,
  breadcrumb,
  status,
  eyebrowIcon: EyebrowIcon,
  eyebrow,
  title,
  description,
  actions,
}: PageHeadingProps) {
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
              {breadcrumb}
            </li>
          </ol>
        </nav>
        {status && (
          <span className="inline-flex items-center gap-2 rounded-full bg-surface-container px-4 py-1 text-xs font-semibold leading-4 tracking-tight text-primary-950 shadow-xs">
            <span
              aria-hidden="true"
              className="size-2.5 rounded-full bg-on-tertiary-container"
            />
            {status}
          </span>
        )}
      </div>

      <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex max-w-2xl flex-col gap-1 xl:max-w-[46%]">
          <p className="flex items-center gap-1 text-xs font-semibold leading-4 tracking-[0.6px] text-muted-foreground uppercase">
            <EyebrowIcon
              aria-hidden="true"
              className="size-5 shrink-0 text-primary-950"
            />
            {eyebrow}
          </p>
          <h1
            id={headingId}
            className="text-headline-lg-mobile font-bold tracking-tight sm:text-headline-lg sm:font-bold sm:tracking-[-0.8px]"
          >
            {title}
          </h1>
          <p className="text-body-md text-on-surface-variant">{description}</p>
        </div>
        {actions && (
          <div className="flex shrink-0 items-center gap-2">{actions}</div>
        )}
      </div>
    </>
  );
}
