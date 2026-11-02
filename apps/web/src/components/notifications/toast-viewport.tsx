import { X } from "@react-zero-ui/icon-sprite";
import { Link } from "@tanstack/react-router";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import {
  type AppNotification,
  useNotificationStore,
} from "@/stores/notification-store";
import { notificationStyle } from "./notification-style";

const DURATION_MS = {
  error: 10_000,
  warning: 8_000,
  success: 6_000,
  info: 6_000,
};

function Toast({ notification }: { notification: AppNotification }) {
  const dismiss = useNotificationStore((state) => state.dismissToast);
  const markRead = useNotificationStore((state) => state.markRead);
  const reducedMotion = useReducedMotion();
  const style = notificationStyle[notification.kind];
  const Icon = style.icon;

  const content = (
    <>
      <span
        className={`flex size-9 shrink-0 items-center justify-center rounded-full ${style.badge}`}
      >
        <Icon aria-hidden="true" className="size-5" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block text-sm font-semibold leading-5 text-primary-950">
          {notification.title}
        </span>
        {notification.description && (
          <span className="mt-0.5 block text-xs leading-4 text-on-surface-variant">
            {notification.description}
          </span>
        )}
      </span>
    </>
  );

  return (
    <motion.li
      layout={!reducedMotion}
      initial={
        reducedMotion ? { opacity: 0 } : { opacity: 0, x: 56, scale: 0.95 }
      }
      animate={{ opacity: 1, x: 0, scale: 1 }}
      exit={
        reducedMotion
          ? { opacity: 0 }
          : { opacity: 0, x: 56, scale: 0.95, transition: { duration: 0.18 } }
      }
      transition={{ type: "spring", stiffness: 420, damping: 32 }}
      role={notification.kind === "error" ? "alert" : "status"}
      className="group pointer-events-auto relative overflow-hidden rounded-xl border border-outline-variant/60 bg-card shadow-lg"
    >
      <div className="flex items-start gap-3 p-3 pr-9">
        {notification.href ? (
          <Link
            to={notification.href}
            onClick={() => {
              markRead(notification.id);
              dismiss(notification.id);
            }}
            className="flex min-w-0 flex-1 items-start gap-3 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            {content}
          </Link>
        ) : (
          <div className="flex min-w-0 flex-1 items-start gap-3">{content}</div>
        )}
      </div>
      <button
        type="button"
        aria-label="Fechar notificação"
        onClick={() => dismiss(notification.id)}
        className="absolute top-2 right-2 flex size-6 items-center justify-center rounded-md text-on-surface-variant outline-none hover:bg-surface-container focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <X aria-hidden="true" className="size-3.5" />
      </button>
      <span
        aria-hidden="true"
        onAnimationEnd={() => dismiss(notification.id)}
        className={`absolute inset-x-0 bottom-0 h-0.5 origin-left motion-reduce:opacity-0 group-hover:[animation-play-state:paused] group-focus-within:[animation-play-state:paused] ${style.bar}`}
        style={{
          animation: `toast-countdown ${DURATION_MS[notification.kind]}ms linear forwards`,
        }}
      />
    </motion.li>
  );
}

/** Toasts de término de processo, no canto da tela. O histórico completo fica no sino do cabeçalho. */
export function ToastViewport() {
  const notifications = useNotificationStore((state) => state.notifications);
  const toastIds = useNotificationStore((state) => state.toastIds);
  const visible = toastIds
    .map((id) => notifications.find((item) => item.id === id))
    .filter((item): item is AppNotification => item !== undefined);

  return (
    <section
      aria-label="Avisos de processos concluídos"
      className="pointer-events-none fixed right-4 bottom-4 z-60 w-[min(24rem,calc(100vw-2rem))]"
    >
      <ul className="flex flex-col-reverse gap-2">
        <AnimatePresence initial={false}>
          {visible.map((notification) => (
            <Toast key={notification.id} notification={notification} />
          ))}
        </AnimatePresence>
      </ul>
    </section>
  );
}
