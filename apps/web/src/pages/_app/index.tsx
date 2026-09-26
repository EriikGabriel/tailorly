import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/_app/")({
  component: App,
});

function App() {
  return <main className="min-h-[calc(100dvh-4rem)]" />;
}
