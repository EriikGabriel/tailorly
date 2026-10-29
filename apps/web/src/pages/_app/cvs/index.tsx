import { CvHistoryList } from "@components/cv-history-list";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_app/cvs/")({
  component: CVsPage,
});

function CVsPage() {
  return (
    <main className="min-h-[calc(100dvh-4rem)] px-gutter py-12">
      <h1 className="text-3xl font-semibold text-foreground">
        Meus Currículos
      </h1>
      <p className="mt-3 mb-6 text-on-surface-variant">
        Cada currículo registra o Ground Truth, a versão da vaga e o template
        usados. Visitantes veem aqui só o que geraram nesta sessão, que expira.
      </p>
      <CvHistoryList />
    </main>
  );
}
