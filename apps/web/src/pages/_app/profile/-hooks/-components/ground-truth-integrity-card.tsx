import { Button } from "@animate/buttons/button";
import {
  ArrowRight,
  FileText,
  RefreshCw,
  ShieldCheck,
  ShieldPlus,
} from "@react-zero-ui/icon-sprite";
import { previewGroundTruthStatus } from "@/data/profile-preview";
import { FactualFidelityGauge } from "./factual-fidelity-gauge";

export function GroundTruthIntegrityCard() {
  const status = previewGroundTruthStatus;

  return (
    <div className="flex w-full flex-col gap-4 rounded-xl bg-card p-6 shadow-xs">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-secondary-container text-on-secondary-container">
            <ShieldCheck aria-hidden="true" className="size-4" />
          </span>
          <div>
            <h2 className="text-title-md font-semibold text-primary-950">
              Ground Truth Primário
            </h2>
            <p className="text-label-sm text-on-surface-variant">
              Fonte imutável de fatos profissionais
            </p>
          </div>
        </div>
        <span
          aria-hidden="true"
          className="size-3 shrink-0 rounded-full bg-on-secondary-container"
        />
      </div>

      <div className="flex flex-col gap-2 rounded-lg bg-surface-container p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2 min-w-0">
            <FileText
              aria-hidden="true"
              className="size-5 shrink-0 text-on-surface-variant"
            />
            <div className="min-w-0">
              <p className="truncate text-label-md font-semibold text-primary-950">
                {status.fileName}
              </p>
              <p className="text-label-sm text-on-surface-variant">
                {status.updatedAtLabel} • {status.sizeLabel}
              </p>
            </div>
          </div>
          <span
            aria-hidden="true"
            className="size-4 shrink-0 rounded-full bg-on-secondary-container/20"
          />
        </div>

        <div className="flex items-center justify-between pt-2 text-label-sm text-on-surface-variant">
          <span>{status.yearsOfExperience} anos de experiência</span>
          <span aria-hidden="true">•</span>
          <span>{status.validatedSkills} competências validadas</span>
        </div>

        <div className="flex items-center gap-2 rounded-md bg-card p-2">
          <ShieldPlus
            aria-hidden="true"
            className="size-3.5 shrink-0 text-primary-container"
          />
          <p className="text-label-sm font-medium text-primary-container">
            100% Protegido contra alucinações de IA
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4 rounded-lg bg-surface-container-low p-2">
        <FactualFidelityGauge value={status.factualFidelity} />
        <div>
          <p className="text-title-md font-semibold text-primary-950">
            Taxa de Fidelidade Factual
          </p>
          <p className="text-body-sm text-on-surface-variant">
            Nenhum dado é inferido além dos registros aprovados por você.
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-2">
        <Button size="lg" className="w-full">
          <FileText aria-hidden="true" className="size-4" />
          Abrir Curadoria &amp; Auditoria Factual
          <ArrowRight aria-hidden="true" className="size-3.5" />
        </Button>

        <div className="flex items-center justify-between px-2 text-label-sm text-on-surface-variant">
          <span>
            Taxa de Fidelidade:{" "}
            <span className="text-on-secondary-container">
              {status.fidelityRate}%
            </span>
          </span>
          <span className="inline-flex items-center gap-1">
            <span
              aria-hidden="true"
              className="size-2 rounded-full bg-on-secondary-container"
            />
            {status.approvedSections}/{status.totalSections} seções aprovadas
          </span>
        </div>

        <button
          type="button"
          className="inline-flex items-center justify-center gap-1.5 rounded-md py-1 text-label-sm font-semibold text-on-surface-variant hover:text-primary-950"
        >
          <RefreshCw aria-hidden="true" className="size-3.5" />
          Substituir / Atualizar CV Base
        </button>
      </div>
    </div>
  );
}
