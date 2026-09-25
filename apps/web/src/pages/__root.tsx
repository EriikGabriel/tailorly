import type { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  HeadContent,
  Outlet,
} from "@tanstack/react-router";
import { TooltipProvider } from "@/components/ui/tooltip";

interface RouterContext {
  queryClient: QueryClient;
}

export const Route = createRootRouteWithContext<RouterContext>()({
  head: () => ({
    meta: [
      { title: "Tailorly" },
      {
        name: "description",
        content: "Currículos personalizados para cada oportunidade.",
      },
      {
        name: "keywords",
        content: "currículo, carreira, vaga, currículo personalizado",
      },
    ],
  }),
  component: RootComponent,
});

function RootComponent() {
  return (
    <TooltipProvider>
      <HeadContent />
      <Outlet />
    </TooltipProvider>
  );
}
