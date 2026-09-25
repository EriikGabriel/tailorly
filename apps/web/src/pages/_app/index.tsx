import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_app/")({
  component: App,
});

function App() {
  return <main className="min-h-dvh">Teste</main>;
}
