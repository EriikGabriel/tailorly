import { PagePlaceholder } from "@components/page-placeholder";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_app/generator")({
  component: GeneratorPage,
});

function GeneratorPage() {
  return <PagePlaceholder title="Gerador" />;
}
