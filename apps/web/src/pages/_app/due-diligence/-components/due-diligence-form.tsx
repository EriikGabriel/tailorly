import { Button } from "@animate/buttons/button";
import { controlClass } from "@components/fields/field-styles";
import { IconInput } from "@components/fields/icon-input";
import { apiErrorMessage } from "@lib/api/error-message";
import { useCreateDueDiligence } from "@queries/due-diligence";
import { useJobPostingList } from "@queries/job-posting";
import {
  Briefcase,
  Building2,
  Plus,
  ScanSearch,
} from "@react-zero-ui/icon-sprite";
import { useState } from "react";

interface DueDiligenceFormProps {
  onCreated: (id: string) => void;
}

/** Pede uma análise por nome de empresa ou a partir de uma vaga salva; cada pedido gera um relatório novo. */
export function DueDiligenceForm({ onCreated }: DueDiligenceFormProps) {
  const create = useCreateDueDiligence();
  const jobs = useJobPostingList();
  const [company, setCompany] = useState("");
  const [jobPostingId, setJobPostingId] = useState("");

  const selectedJob = jobs.data?.find((job) => job.id === jobPostingId);
  const companyName = company.trim() || selectedJob?.companyName?.trim() || "";
  const canSubmit = companyName.length >= 2 && !create.isPending;

  function submit() {
    if (!canSubmit) return;
    create.mutate(
      {
        companyName: company.trim() || undefined,
        jobPostingId: jobPostingId || undefined,
      },
      { onSuccess: (created) => onCreated(created.id) },
    );
  }

  return (
    <div className="flex flex-col gap-3 rounded-xl bg-card p-4 shadow-xs">
      <h2 className="flex items-center gap-2 text-base font-semibold text-primary-950">
        <Plus aria-hidden="true" className="size-4" />
        Nova análise
      </h2>

      <div className="flex flex-col gap-1">
        <label
          htmlFor="dd-job"
          className="text-xs font-semibold tracking-[0.3px] text-on-surface-variant"
        >
          Vaga de origem (opcional)
        </label>
        <div className={controlClass}>
          <Briefcase
            aria-hidden="true"
            className="size-4 shrink-0 text-secondary-700"
          />
          <select
            id="dd-job"
            value={jobPostingId}
            onChange={(event) => setJobPostingId(event.target.value)}
            className="min-w-0 flex-1 bg-transparent py-2 text-sm font-medium text-primary-950 outline-none"
          >
            <option value="">Nenhuma vaga</option>
            {(jobs.data ?? []).map((job) => (
              <option key={job.id} value={job.id}>
                {job.title ?? "Vaga sem título"}
                {job.companyName ? ` — ${job.companyName}` : ""}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex flex-col gap-1">
        <label
          htmlFor="dd-company"
          className="text-xs font-semibold tracking-[0.3px] text-on-surface-variant"
        >
          Empresa
        </label>
        <IconInput
          id="dd-company"
          icon={Building2}
          value={company}
          onChange={setCompany}
          maxLength={200}
          placeholder={selectedJob?.companyName ?? "Nome da empresa"}
          onEnter={submit}
        />
      </div>

      <Button type="button" disabled={!canSubmit} onClick={submit}>
        <ScanSearch aria-hidden="true" className="size-4" />
        {create.isPending
          ? "Consultando fontes públicas..."
          : "Analisar empresa"}
      </Button>

      {create.isPending && (
        <p className="text-xs text-on-surface-variant">
          Buscamos fontes para cada um dos onze fatores. Isso pode levar até um
          minuto.
        </p>
      )}
      {create.isError && (
        <p role="alert" className="text-body-sm text-error">
          {apiErrorMessage(
            create.error,
            "Não foi possível concluir a due diligence.",
          )}
        </p>
      )}
    </div>
  );
}
