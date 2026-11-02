import assert from "node:assert/strict";
import { beforeEach, test } from "node:test";
import {
  notify,
  useNotificationStore as store,
} from "../../src/stores/notification-store";
import { resetClientState } from "../../src/stores/reset-client-state";

beforeEach(() => store.getState().clear());

test("cada aviso vira toast visível e item não lido do histórico", () => {
  const id = notify({ kind: "success", title: "Currículo gerado" });

  assert.deepEqual(store.getState().toastIds, [id]);
  assert.equal(store.getState().notifications[0]?.read, false);
});

test("só os três toasts mais recentes ficam na tela, mas o histórico guarda todos", () => {
  for (let i = 0; i < 5; i++) notify({ kind: "info", title: `Aviso ${i}` });

  assert.equal(store.getState().toastIds.length, 3);
  assert.equal(store.getState().notifications.length, 5);
});

test("fechar o toast não apaga nem marca o aviso como lido", () => {
  const id = notify({ kind: "error", title: "Falhou" });
  store.getState().dismissToast(id);

  assert.deepEqual(store.getState().toastIds, []);
  assert.equal(store.getState().notifications[0]?.read, false);
});

test("marcar como lidas e remover atualizam o histórico", () => {
  const first = notify({ kind: "success", title: "A" });
  notify({ kind: "warning", title: "B" });
  store.getState().markAllRead();
  assert.ok(store.getState().notifications.every((item) => item.read));

  store.getState().remove(first);
  assert.equal(store.getState().notifications.length, 1);
});

test("o histórico é limitado e some ao sair da conta", () => {
  for (let i = 0; i < 60; i++) notify({ kind: "info", title: `Aviso ${i}` });
  assert.equal(store.getState().notifications.length, 50);

  resetClientState();
  assert.equal(store.getState().notifications.length, 0);
});
