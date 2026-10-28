import type { IconProps } from "@react-zero-ui/icon-sprite";
import type { ComponentType, ReactNode } from "react";
import { bareInputClass, controlClass, iconClass } from "./field-styles";

export interface IconInputProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  icon?: ComponentType<IconProps>;
  type?: "text" | "email" | "tel" | "url";
  placeholder?: string;
  inputMode?: "text" | "email" | "tel" | "url" | "numeric";
  autoComplete?: string;
  maxLength?: number;
  trailing?: ReactNode;
  invalid?: boolean;
  disabled?: boolean;
  describedBy?: string;
  /** Chamado ao pressionar Enter no campo (sem enviar formulários). */
  onEnter?: () => void;
}

export function IconInput({
  id,
  value,
  onChange,
  icon: Icon,
  type = "text",
  placeholder = "Não informado",
  inputMode,
  autoComplete,
  maxLength,
  trailing,
  invalid,
  disabled,
  describedBy,
  onEnter,
}: IconInputProps) {
  return (
    <div
      className={controlClass}
      aria-invalid={invalid || undefined}
      data-disabled={disabled ? "" : undefined}
    >
      {Icon && <Icon aria-hidden="true" className={iconClass} />}
      <input
        id={id}
        type={type}
        value={value}
        inputMode={inputMode}
        autoComplete={autoComplete}
        maxLength={maxLength}
        placeholder={placeholder}
        disabled={disabled}
        aria-invalid={invalid || undefined}
        aria-describedby={describedBy}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter" && onEnter) {
            event.preventDefault();
            onEnter();
          }
        }}
        className={bareInputClass}
      />
      {trailing}
    </div>
  );
}
