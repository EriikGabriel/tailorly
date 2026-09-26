import { Tooltip as TooltipPrimitive } from "@base-ui/react/tooltip";
import { Header } from "@components/header";
import type { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  HeadContent,
  Outlet,
} from "@tanstack/react-router";

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
    <TooltipPrimitive.Provider delay={0}>
      <HeadContent />
      <Header />
      <Outlet />
    </TooltipPrimitive.Provider>
  );
}
