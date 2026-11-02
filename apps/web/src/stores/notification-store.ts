import { create } from "zustand";

/** Rotas que um aviso pode abrir; precisa ser subconjunto das rotas registradas do app. */
export type NotificationHref =
  | "/"
  | "/base"
  | "/cvs"
  | "/due-diligence"
  | "/generator"
  | "/profile";

export type NotificationKind = "success" | "error" | "warning" | "info";

export interface Notice {
  kind: NotificationKind;
  title: string;
  description?: string;
  /** Rota aberta ao clicar na notificação. */
  href?: NotificationHref;
}

export interface AppNotification extends Notice {
  id: string;
  createdAt: number;
  read: boolean;
}

const MAX_NOTIFICATIONS = 50;

interface NotificationState {
  notifications: AppNotification[];
  /** Ids que estão visíveis como toast agora; o histórico fica em `notifications`. */
  toastIds: string[];
  notify: (notice: Notice) => string;
  dismissToast: (id: string) => void;
  markRead: (id: string) => void;
  markAllRead: () => void;
  remove: (id: string) => void;
  clear: () => void;
}

/**
 * Notificações do app: cada término de processo vira um toast e um item do histórico do sino. É só estado
 * de interface, em memória: não guarda fatos nem cota e some ao recarregar a página ou sair da conta.
 */
export const useNotificationStore = create<NotificationState>()((set) => ({
  notifications: [],
  toastIds: [],
  notify: (notice) => {
    const id = crypto.randomUUID();
    set((state) => ({
      notifications: [
        { ...notice, id, createdAt: Date.now(), read: false },
        ...state.notifications,
      ].slice(0, MAX_NOTIFICATIONS),
      // No máximo três toasts na tela; os mais antigos continuam no sino.
      toastIds: [id, ...state.toastIds].slice(0, 3),
    }));
    return id;
  },
  dismissToast: (id) =>
    set((state) => ({
      toastIds: state.toastIds.filter((item) => item !== id),
    })),
  markRead: (id) =>
    set((state) => ({
      notifications: state.notifications.map((item) =>
        item.id === id ? { ...item, read: true } : item,
      ),
    })),
  markAllRead: () =>
    set((state) => ({
      notifications: state.notifications.map((item) => ({
        ...item,
        read: true,
      })),
    })),
  remove: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((item) => item.id !== id),
      toastIds: state.toastIds.filter((item) => item !== id),
    })),
  clear: () => set({ notifications: [], toastIds: [] }),
}));

/** Atalho para disparar fora de componentes React. */
export function notify(notice: Notice): string {
  return useNotificationStore.getState().notify(notice);
}
