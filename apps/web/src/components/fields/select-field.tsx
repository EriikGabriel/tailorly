import { Select } from "@base-ui-components/react/select";
import { Check, ChevronDown, type IconProps } from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";
import { controlClass, iconClass } from "./field-styles";

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectFieldProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  options: readonly SelectOption[];
  icon?: ComponentType<IconProps>;
  placeholder?: string;
  describedBy?: string;
  disabled?: boolean;
}

const NONE = "__none__";

/** Lista de opções; um valor extraído que não esteja na lista continua selecionável. */
export function SelectField({
  id,
  value,
  onChange,
  options,
  icon: Icon,
  placeholder = "Não informado",
  describedBy,
  disabled,
}: SelectFieldProps) {
  const known = options.some((option) => option.value === value);
  const items: SelectOption[] = [
    { value: NONE, label: placeholder },
    ...options,
    ...(value && !known ? [{ value, label: value }] : []),
  ];

  return (
    <Select.Root
      value={value === "" ? NONE : value}
      onValueChange={(next) =>
        onChange(next === NONE || next == null ? "" : next)
      }
      disabled={disabled}
    >
      <Select.Trigger
        id={id}
        aria-describedby={describedBy}
        className={`${controlClass} justify-between text-left data-popup-open:border-primary-900`}
      >
        <span className="flex min-w-0 items-center gap-2">
          {Icon && <Icon aria-hidden="true" className={iconClass} />}
          <Select.Value className="truncate">
            {(selected: string) =>
              selected === NONE || !selected ? (
                <span className="font-normal text-outline">{placeholder}</span>
              ) : (
                (items.find((item) => item.value === selected)?.label ??
                selected)
              )
            }
          </Select.Value>
        </span>
        <Select.Icon className="shrink-0 text-on-surface-variant transition-transform duration-200 data-popup-open:rotate-180">
          <ChevronDown aria-hidden="true" className="size-4" />
        </Select.Icon>
      </Select.Trigger>
      <Select.Portal>
        <Select.Positioner
          alignItemWithTrigger={false}
          sideOffset={6}
          className="z-70"
        >
          <Select.Popup className="w-(--anchor-width) min-w-48 origin-(--transform-origin) rounded-lg border border-outline-variant/60 bg-card p-1 text-primary-950 shadow-xl outline-none transition-[opacity,transform] duration-150 data-starting-style:translate-y-1 data-starting-style:opacity-0 data-ending-style:opacity-0">
            <Select.List>
              {items.map((item) => (
                <Select.Item
                  key={item.value}
                  value={item.value}
                  className="flex cursor-pointer items-center justify-between gap-3 rounded-md px-3 py-2 text-sm outline-none transition-colors data-highlighted:bg-surface-container data-selected:bg-primary data-selected:text-primary-foreground"
                >
                  <Select.ItemText>{item.label}</Select.ItemText>
                  <Select.ItemIndicator>
                    <Check aria-hidden="true" className="size-4" />
                  </Select.ItemIndicator>
                </Select.Item>
              ))}
            </Select.List>
          </Select.Popup>
        </Select.Positioner>
      </Select.Portal>
    </Select.Root>
  );
}
