import {
  Popover,
  PopoverPanel,
  PopoverTitle,
  PopoverTrigger,
} from "@animate/base/popover";
import { Button } from "@animate/buttons/button";
import {
  Bell,
  BellOff,
  CheckCheck,
  Trash2,
  X,
} from "@react-zero-ui/icon-sprite";
import {
  type AppNotification,
  useNotificationStore,
} from "@stores/notification-store";
import { Link } from "@tanstack/react-router";
import { motion, useReducedMotion } from "motion/react";
import { useState } from "react";
import { notificationStyle } from "./notification-style";

const relative = new Intl.RelativeTimeFormat("pt-BR", { numeric: "auto" });

function timeAgo(timestamp: number, now: number): string {
  const seconds = Math.round((timestamp - now) / 1000);
  if (Math.abs(seconds) < 45) return "agora";
  const minutes = Math.round(seconds / 60);
  if (Math.abs(minutes) < 60) return relative.format(minutes, "minute");
  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) return relative.format(hours, "hour");
  return relative.format(Math.round(hours / 24), "day");
}

function Item({
  notification,
  now,
  onNavigate,
}: {
  notification: AppNotification;
  now: number;
  onNavigate: () => void;
}) {
  const markRead = useNotificationStore((state) => state.markRead);
  const remove = useNotificationStore((state) => state.remove);
  const style = notificationStyle[notification.kind];
  const Icon = style.icon;

  const body = (
    <>
      <span
        className={`mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-full ${style.badge}`}
      >
        <Icon aria-hidden="true" className="size-4" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="flex items-center gap-2">
          <span className="truncate text-sm font-semibold text-primary-950">
            {notification.title}
          </span>
          {!notification.read && (
            <span
              role="img"
              aria-label="Não lida"
              className="size-2 shrink-0 rounded-full bg-on-tertiary-container"
            />
          )}
        </span>
        {notification.description && (
          <span className="block text-xs text-on-surface-variant">
            {notification.description}
          </span>
        )}
        <span className="mt-0.5 block text-[11px] text-outline">
          {style.label} · {timeAgo(notification.createdAt, now)}
        </span>
      </span>
    </>
  );

  return (
    <li className="group relative flex items-start gap-1 rounded-lg hover:bg-surface-container-low">
      {notification.href ? (
        <Link
          to={notification.href}
          onClick={() => {
            markRead(notification.id);
            onNavigate();
          }}
          className="flex min-w-0 flex-1 items-start gap-3 rounded-lg p-2 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          {body}
        </Link>
      ) : (
        <button
          type="button"
          onClick={() => markRead(notification.id)}
          className="flex min-w-0 flex-1 items-start gap-3 rounded-lg p-2 text-left outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        >
          {body}
        </button>
      )}
      <button
        type="button"
        aria-label="Remover notificação"
        onClick={() => remove(notification.id)}
        className="mt-2 mr-1 flex size-6 shrink-0 items-center justify-center rounded-md text-on-surface-variant opacity-0 outline-none group-hover:opacity-100 hover:bg-surface-container focus-visible:opacity-100 focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <X aria-hidden="true" className="size-3.5" />
      </button>
    </li>
  );
}

/** Sino do cabeçalho: contador de não lidas e o histórico dos processos que terminaram. */
export function NotificationBell() {
  const notifications = useNotificationStore((state) => state.notifications);
  const markAllRead = useNotificationStore((state) => state.markAllRead);
  const clear = useNotificationStore((state) => state.clear);
  const reducedMotion = useReducedMotion();
  const [open, setOpen] = useState(false);
  const unread = notifications.filter((item) => !item.read).length;
  const latestUnread = notifications.find((item) => !item.read)?.id;
  const now = Date.now();

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            type="button"
            variant="ghost"
            className="relative"
            aria-label={
              unread > 0 ? `Notificações, ${unread} não lidas` : "Notificações"
            }
          >
            {/* A chave faz o sino balançar a cada novo aviso não lido. */}
            <motion.span
              key={latestUnread ?? "idle"}
              animate={
                reducedMotion || !latestUnread
                  ? undefined
                  : { rotate: [0, -18, 14, -10, 6, 0] }
              }
              transition={{ duration: 0.6 }}
              className="inline-flex origin-top"
            >
              <Bell aria-hidden="true" className="size-5" />
            </motion.span>
            {unread > 0 && (
              <span
                aria-hidden="true"
                className="absolute top-0.5 right-0.5 flex min-w-4 items-center justify-center rounded-full bg-error px-1 text-[10px] font-bold leading-4 text-on-error"
              >
                {unread > 9 ? "9+" : unread}
              </span>
            )}
          </Button>
        }
      />
      <PopoverPanel
        align="end"
        sideOffset={8}
        className="w-96 max-w-[calc(100vw-2rem)] p-0"
      >
        <div className="flex items-center justify-between gap-2 border-b border-outline-variant/60 px-4 py-3">
          <PopoverTitle className="text-sm font-semibold text-primary-950">
            Notificações
          </PopoverTitle>
          <div className="flex items-center gap-1">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              disabled={unread === 0}
              onClick={markAllRead}
            >
              <CheckCheck aria-hidden="true" className="size-4" />
              Marcar como lidas
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              aria-label="Limpar notificações"
              disabled={notifications.length === 0}
              onClick={clear}
            >
              <Trash2 aria-hidden="true" className="size-4" />
            </Button>
          </div>
        </div>

        {notifications.length === 0 ? (
          <div className="flex flex-col items-center gap-2 px-6 py-10 text-center">
            <BellOff aria-hidden="true" className="size-6 text-outline" />
            <p className="text-sm text-on-surface-variant">
              Nada por aqui ainda. Avisamos quando a extração do currículo, a
              leitura da vaga, a due diligence ou a geração terminarem.
            </p>
          </div>
        ) : (
          <ul className="max-h-96 space-y-0.5 overflow-y-auto p-2">
            {notifications.map((notification) => (
              <Item
                key={notification.id}
                notification={notification}
                now={now}
                onNavigate={() => setOpen(false)}
              />
            ))}
          </ul>
        )}
        <p className="border-t border-outline-variant/60 px-4 py-2 text-[11px] text-outline">
          O histórico vale só para esta aba e some ao recarregar.
        </p>
      </PopoverPanel>
    </Popover>
  );
}
