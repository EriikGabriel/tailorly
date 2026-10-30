import { useActiveGroundTruth } from "@queries/ground-truth";
import { ArrowRight, IdCard } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import { describeLink, formatPhone } from "@/utils/review-format";
import { SectionCard } from "./section-card";

export function IdentitySection() {
  const { isLoading, hasActiveSnapshot, facts } = useActiveGroundTruth();
  const identity = facts?.identity;

  return (
    <SectionCard
      icon={IdCard}
      title="1. Identidade & Dados Profissionais"
      description="Vêm direto do Currículo Base aprovado (Ground Truth): a única fonte de fatos pessoais. Para corrigir algo aqui, revise o Currículo Base."
      trailing={
        <span className="rounded-full bg-surface-container px-2.5 py-1 text-label-sm font-medium text-on-secondary-container">
          Somente leitura
        </span>
      }
    >
      {isLoading ? (
        <p className="text-body-sm text-on-surface-variant">Carregando...</p>
      ) : !hasActiveSnapshot ? (
        <EmptyIdentityState />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2">
          <ReadOnlyField label="Nome Completo" value={identity?.name} />
          <ReadOnlyField label="E-mail" value={identity?.email} />
          <ReadOnlyField
            label="Telefone / WhatsApp"
            value={identity?.phone ? formatPhone(identity.phone) : undefined}
          />
          <ReadOnlyField
            label="Cargo Atual / Alvo Principal"
            value={identity?.headline}
          />
          <div className="sm:col-span-2">
            <ReadOnlyField
              label="Localização & Mobilidade"
              value={identity?.location}
            />
          </div>
          <LinkField label="LinkedIn" value={identity?.linkedin} />
          <LinkField label="GitHub / Portfólio" value={identity?.github} />
        </div>
      )}
    </SectionCard>
  );
}

function EmptyIdentityState() {
  return (
    <div className="flex flex-col items-start gap-3 rounded-lg bg-surface-container-low p-4">
      <p className="text-body-sm text-on-surface-variant">
        Sem Ground Truth ativo nesta conta. Envie e aprove um Currículo Base
        para preencher sua identidade e dados profissionais aqui.
      </p>
      <Link
        to="/base"
        className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-label-md font-semibold text-primary-foreground hover:bg-primary/90"
      >
        Ir para o Currículo Base
        <ArrowRight aria-hidden="true" className="size-3.5" />
      </Link>
    </div>
  );
}

function ReadOnlyField({
  label,
  value,
}: {
  label: string;
  value: string | undefined;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-label-md font-medium text-primary-950">
        {label}
      </span>
      <p className="w-full truncate rounded-lg bg-surface-container-low px-4 py-2.5 text-body-md text-primary-950">
        {value ?? (
          <span className="text-on-surface-variant">Não informado</span>
        )}
      </p>
    </div>
  );
}

function LinkField({
  label,
  value,
}: {
  label: string;
  value: string | undefined;
}) {
  const link = describeLink(value);
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-label-md font-medium text-primary-950">
        {label}
      </span>
      <div className="w-full truncate rounded-lg bg-surface-container-low px-4 py-2.5 text-body-md">
        {link ? (
          <a
            href={link.href}
            target="_blank"
            rel="noreferrer"
            className="text-primary-container hover:underline"
          >
            {link.display}
          </a>
        ) : (
          <span className="text-on-surface-variant">Não informado</span>
        )}
      </div>
    </div>
  );
}
