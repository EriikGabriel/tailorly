import { Button } from "@animate/buttons/button";
import { ShieldCheck } from "@react-zero-ui/icon-sprite";

export function ReviewCompletionCard() {
  return (
    <section
      aria-labelledby="review-completion-heading"
      className="space-y-4 rounded-2xl bg-card p-4 shadow-md sm:p-6"
    >
      <div className="flex items-center gap-4">
        <span className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary text-primary-foreground">
          <ShieldCheck aria-hidden="true" className="size-6" />
        </span>
        <div>
          <h2 id="review-completion-heading" className="text-title-md">
            Auditoria factual aguardando revisão
          </h2>
          <p id="review-completion-description" className="text-body-sm">
            Salvar e aprovar estarão disponíveis após a integração com o
            processamento e a revisão do documento.
          </p>
        </div>
      </div>
      <div className="flex flex-wrap gap-2">
        <Button
          disabled
          variant="accent"
          aria-describedby="review-completion-description"
          className="h-auto min-h-12 whitespace-normal rounded-xl px-6 py-3.5"
        >
          Salvar Rascunho da Auditoria
        </Button>
        <Button
          disabled
          aria-describedby="review-completion-description"
          className="h-auto min-h-12 whitespace-normal rounded-xl px-6 py-3.5"
        >
          Aprovar Ground Truth e Voltar ao Início →
        </Button>
      </div>
    </section>
  );
}
