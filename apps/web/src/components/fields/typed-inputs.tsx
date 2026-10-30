import {
  ExternalLink,
  Github,
  Globe,
  Linkedin,
  Mail,
  Phone,
} from "@react-zero-ui/icon-sprite";
import {
  describeLink,
  formatPhone,
  type LinkKind,
} from "@/utils/review-format";
import { IconInput, type IconInputProps } from "./icon-input";

type TypedProps = Omit<IconInputProps, "icon" | "type" | "inputMode">;

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function EmailInput(props: TypedProps) {
  const invalid = props.value.trim() !== "" && !EMAIL.test(props.value.trim());
  return (
    <IconInput
      {...props}
      icon={Mail}
      type="email"
      inputMode="email"
      autoComplete="email"
      invalid={invalid}
    />
  );
}

/** Máscara de telefone brasileiro enquanto digita; números internacionais ficam intactos. */
export function PhoneInput({ onChange, ...props }: TypedProps) {
  return (
    <IconInput
      {...props}
      icon={Phone}
      type="tel"
      inputMode="tel"
      autoComplete="tel"
      placeholder="(11) 98765-4321"
      onChange={(value) => onChange(formatPhone(value))}
    />
  );
}

const linkIcons = { github: Github, linkedin: Linkedin, other: Globe } as const;

export function LinkInput(props: TypedProps) {
  const info = describeLink(props.value);
  const kind: LinkKind = info?.kind ?? "other";
  return (
    <IconInput
      {...props}
      icon={linkIcons[kind]}
      type="text"
      inputMode="url"
      placeholder="github.com/usuario"
      invalid={props.value.trim() !== "" && info === null}
      trailing={
        info && (
          <a
            href={info.href}
            target="_blank"
            rel="noreferrer noopener"
            aria-label={`Abrir ${info.display} em nova aba`}
            className="-mr-1 rounded-md p-1 text-secondary-700 hover:bg-surface-container focus-visible:outline-2 focus-visible:outline-primary-900"
          >
            <ExternalLink aria-hidden="true" className="size-4" />
          </a>
        )
      }
    />
  );
}
