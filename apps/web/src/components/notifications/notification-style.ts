import {
  CircleAlert,
  CircleCheck,
  Info,
  TriangleAlert,
} from "@react-zero-ui/icon-sprite";
import type { NotificationKind } from "@/stores/notification-store";

/** Ícone e cores de cada tipo de aviso, usando os tokens do tema (êxito em verde, igual à nota de ATS). */
export const notificationStyle: Record<
  NotificationKind,
  { icon: typeof Info; badge: string; bar: string; label: string }
> = {
  success: {
    icon: CircleCheck,
    badge: "bg-[hsl(140_45%_91%)] text-[hsl(140_60%_26%)]",
    bar: "bg-[hsl(140_55%_34%)]",
    label: "Concluído",
  },
  error: {
    icon: CircleAlert,
    badge: "bg-error-container text-on-error-container",
    bar: "bg-error",
    label: "Falhou",
  },
  warning: {
    icon: TriangleAlert,
    badge: "bg-tertiary-100 text-tertiary-700",
    bar: "bg-tertiary-600",
    label: "Atenção",
  },
  info: {
    icon: Info,
    badge: "bg-secondary-100 text-primary-950",
    bar: "bg-primary-500",
    label: "Aviso",
  },
};
