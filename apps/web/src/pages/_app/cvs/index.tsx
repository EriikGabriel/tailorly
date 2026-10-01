import { PagePlaceholder } from "@components/page-placeholder";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_app/cvs/")({
  component: CVsPage,
});

function CVsPage() {
  return <PagePlaceholder title="Meus Currículos" />;
}
