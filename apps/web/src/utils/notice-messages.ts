import { apiErrorMessage } from "@api/error-message";
import type {
  CvGenerationResponse,
  JobPostingVersionResponse,
} from "@api/generated/model";
import type { Notice } from "@stores/notification-store";

/** Avisos de término de processo, no mesmo tom da interface. Os textos de erro vêm da API quando existem. */
export const notices = {
  failure(title: string, error: Error, fallback: string): Notice {
    return {
      kind: "error",
      title,
      description: apiErrorMessage(error, fallback),
    };
  },

  jobRead(version: JobPostingVersionResponse): Notice {
    const title = version.details?.title ?? "Vaga";
    const company = version.details?.company?.name;
    const subject = company ? `${title} — ${company}` : title;
    if (version.analysisStatus === "completed") {
      return {
        kind: "success",
        title: "Vaga lida e analisada",
        description: subject,
        href: "/generator",
      };
    }
    return {
      kind: "warning",
      title: "Vaga lida, sem análise por IA",
      description:
        version.analysisStatus === "quota_exceeded"
          ? `${subject}. O limite de processamento da sessão foi atingido.`
          : `${subject}. A análise de requisitos não pôde ser feita; tente de novo no gerador.`,
      href: "/generator",
    };
  },

  cvGenerated(generation: CvGenerationResponse): Notice {
    const job = generation.job?.title ?? "a vaga";
    const score = generation.ats?.report?.score;
    return {
      kind: "success",
      title: generation.reused
        ? "Currículo já existente reaproveitado"
        : "Currículo gerado",
      description: score === undefined ? job : `${job} · nota ATS ${score}/100`,
      href: "/generator",
    };
  },
};
