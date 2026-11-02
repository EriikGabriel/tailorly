import {
  Lock,
  MessageSquareQuote,
  ShieldCheck,
} from "@react-zero-ui/icon-sprite";

const principles = [
  {
    icon: MessageSquareQuote,
    title: "Relatos não são provas",
    text: "Relatos de pessoas e a ausência de dados não comprovam problemas.",
  },
  {
    icon: Lock,
    title: "Só fontes públicas",
    text: "Fontes privadas dependem de integração e acesso autorizado, ainda não disponíveis.",
  },
  {
    icon: ShieldCheck,
    title: "Ground Truth intacto",
    text: "A análise apoia a sua decisão e nunca altera o Currículo Base.",
  },
];

export function DueDiligencePrinciples() {
  return (
    <ul
      aria-label="Como ler a due diligence"
      className="grid gap-2 md:grid-cols-3"
    >
      {principles.map(({ icon: Icon, title, text }) => (
        <li
          key={title}
          className="flex gap-3 rounded-xl bg-surface-container-low p-3"
        >
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-card text-primary-950 shadow-xs">
            <Icon aria-hidden="true" className="size-4" />
          </span>
          <span className="min-w-0">
            <span className="block text-sm font-semibold text-primary-950">
              {title}
            </span>
            <span className="block text-xs text-on-surface-variant">
              {text}
            </span>
          </span>
        </li>
      ))}
    </ul>
  );
}
