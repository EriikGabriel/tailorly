import { MotionEffect } from "@animate/primitives/effects/motion-effect";
import { Tooltip as TooltipPrimitive } from "@base-ui/react/tooltip";
import { AnonymousMigrationDialog } from "@components/anonymous-migration-dialog";
import { BaseCvRestorer } from "@components/base-cv-restorer";
import { Header } from "@components/header";
import { BackgroundNotifications } from "@components/notifications/background-notifications";
import { ToastViewport } from "@components/notifications/toast-viewport";
import type { QueryClient } from "@tanstack/react-query";
import {
  createRootRouteWithContext,
  HeadContent,
  Outlet,
  useLocation,
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
  const pathname = useLocation({ select: (location) => location.pathname });

  return (
    <TooltipPrimitive.Provider delay={0}>
      <HeadContent />
      <Header />
      {/* Só opacidade: uma transformação no ancestral quebraria elementos fixos das páginas. */}
      <MotionEffect
        key={pathname}
        transition={{ duration: 0.25, ease: "easeOut" }}
      >
        <Outlet />
      </MotionEffect>
      <AnonymousMigrationDialog />
      <BaseCvRestorer />
      <BackgroundNotifications />
      <ToastViewport />
    </TooltipPrimitive.Provider>
  );
}
