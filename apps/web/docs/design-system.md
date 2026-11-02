# Design system

## Tokens

`src/styles.css` tem três blocos: variáveis cruas em `:root` (nomes sem
prefixo), o mapeamento `@theme inline` (que expõe os tokens como utilities do
Tailwind) e os estilos base.

**Paleta "Warm Editorial Executive"** — três famílias com escala de 50 a 950:

| Família     | Papel         | Âncora                                                                              |
| ----------- | ------------- | ----------------------------------------------------------------------------------- |
| `primary`   | Rich Espresso | `--primary-900: var(--material-primary)`, `--primary-950: var(--primary-container)` |
| `secondary` | Warm Walnut   | `--secondary-800: var(--material-secondary)`                                        |
| `tertiary`  | Golden Amber  | `--tertiary-600: var(--material-tertiary)`                                          |

Sobre elas vêm os papéis Material (`surface`, `surface-container-lowest` até
`-highest`, `on-surface*`, `outline*`, `*-container`, `*-fixed`, `error*`, e
`inverse-*`), os semânticos shadcn (`background`, `foreground`, `card`, `popover`,
`primary`, `muted`, `accent`, `destructive`, `border`, `input`, `ring`, `chart-1..5`,
`sidebar*`), mais `--shadow-1..3`, `--radius`, `--gutter` e `--canvas-margin`.

> Ao escolher um tom de superfície use a **escala de container**: `card` para
> superfícies elevadas (correspondendo a `--surface-container-lowest`),
> `bg-surface-container-low` para campos e blocos internos,
> `bg-surface-container` para chrome de fundo. Misturar `card` e
> `bg-white` gera inconsistência — `pages/_app/_home/-sections/steps-section.tsx` ainda usa `bg-white` e é
> um ponto conhecido de inconsistência.

**Tipografia.** Inter Variable, exposta como scale semântica em `@theme inline`:

| Token                             | Tamanho / linha                |
| --------------------------------- | ------------------------------ |
| `display-lg`                      | 3.5rem / 4rem, 700, `-0.025em` |
| `headline-lg`                     | 2rem / 2.5rem, 600             |
| `headline-lg-mobile`              | 1.625rem / 2.125rem, 600       |
| `headline-md`                     | 1.5rem / 2rem, 600             |
| `headline-sm`                     | 1.25rem / 1.75rem, 600         |
| `title-md`                        | 1rem / 1.5rem, 600             |
| `body-lg` / `body-md` / `body-sm` | 1.125 / 0.9375 / 0.8125rem     |
| `label-md` / `label-sm`           | 0.875 / 0.75rem, com tracking  |

**Raios e espaçamento.** `--radius-sm` a `--radius-2xl` derivam de `--radius`
(aritmética `calc`), mais `--radius-full`. Espaçamento inclui `--spacing-gutter`
e `--spacing-space-xs..xl`.

> **Sem `tailwind.config.ts`.** Com Tailwind 4 a configuração é CSS-first: tokens, escala de
> tipografia e raios vivem em `src/styles.css`, que é a fonte da verdade.

## Movimento reduzido

Duas abordagens complementares:

- `useReducedMotion()` de `motion/react` — usado em componentes para zerar
  durações, `initial`/`exit` e `whileTap`/`whileHover`.
- a variante `motion-reduce:` do Tailwind — usada em `transition-colors`,
  `transition-transform` e `animate-ping`.

Componentes animados que **precisam** respeitar as duas:
`review-stepper.tsx`, `review-workspace.tsx`, `review-draft-collection.tsx`,
`review-correction-input.tsx`,
`review-month-picker.tsx`, `base-cv-picker.tsx`, `hero-ingestion-card.tsx`,
`header.tsx`, `review-section.tsx`.

---

## Camadas de componente

```
primitives/  →  Base UI + motion/react  (implementação)
   ↑
base/ buttons/  →  cva + tokens do projeto  (camada de uso)
   ↑
components/     →  header, page-placeholder, badge, noise-texture
```

## `ui/animate/primitives/` — implementação

| Componente              | Base                                                                                                                                              |
| ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| `animate/slot`          | Slot `asChild` sobre `motion`                                                                                                                     |
| `animate/tabs`          | Tabs com highlight animado                                                                                                                        |
| `base/*`                | `alert-dialog`, `checkbox`, `dialog`, `menu`, `progress`, `switch`, `tabs`, `toggle`, `toggle-group`, `tooltip` — primitivos Base UI com animação |
| `buttons/button`        | `motion.button` com `hoverScale` (1.025) e `tapScale` (0.98), neutros sob movimento reduzido                                                      |
| `buttons/ripple`        | Botão com ripple                                                                                                                                  |
| `effects/auto-height`   | Anima altura via `useAutoHeight`                                                                                                                  |
| `effects/highlight`     | Highlight deslizante (`mode="parent"`, `controlledItems`)                                                                                         |
| `texts/counting-number` | Número animado                                                                                                                                    |

## `ui/animate/base/` e `ui/animate/buttons/` — camada de uso

Reexportam os primitivos e aplicam os tokens do projeto via `cva`/`cn`. São
estes que as páginas importam.

- `buttons/button` — variantes `default`, `accent`, `destructive`, `outline`,
  `secondary`, `ghost`, `link`; tamanhos `default`, `sm`, `lg`, `icon`,
  `icon-sm`, `icon-lg`; prop extra `disableZoom` que fixa `hoverScale`/`tapScale`
  em 1 (use em botões dentro de listas densas).
- `base/*` — mesmos primitivos com defaults de layout do projeto.
- `buttons/copy`, `community/share-button` — auxiliares.

> Há duplicação aparente entre `base/tabs.tsx` e `primitives/base/tabs.tsx`: o
> primeiro **importa** o segundo e só injeta classes. O grafo de import é
> sempre `primitives → base`, nunca o inverso.

## Ícones

`@react-zero-ui/icon-sprite` (ícones Lucide e Tabler). O `prebuild` roda
`zero-icons`, que gera `public/icons.svg` **apenas com os ícones importados**; o
Vite o inclui no build. Em desenvolvimento a biblioteca renderiza os SVGs
diretamente. `public/icons.svg` está no `.gitignore` — é gerado, não versionado.

`components.json` declara `iconLibrary: "lucide"` para o gerador shadcn, mas
componentes adicionados por ele devem trocar os imports gerados para
`@react-zero-ui/icon-sprite`.

---
