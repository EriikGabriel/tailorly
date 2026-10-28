import type { DraftFields, ReviewField } from "@@types/review-model";
import { AutoTextarea } from "@components/fields/auto-textarea";
import { FieldFrame } from "@components/fields/field-frame";
import { IconInput } from "@components/fields/icon-input";
import { MonthYearPicker } from "@components/fields/month-year-picker";
import { PeriodField } from "@components/fields/period-field";
import { ProficiencyField } from "@components/fields/proficiency-field";
import { SelectField } from "@components/fields/select-field";
import {
  EmailInput,
  LinkInput,
  PhoneInput,
} from "@components/fields/typed-inputs";
import { useId } from "react";

function renderControl(
  field: ReviewField,
  id: string,
  value: string,
  set: (value: string) => void,
) {
  const describedBy = field.hint ? `${id}-hint` : undefined;
  const common = { id, value, onChange: set, describedBy };

  switch (field.type) {
    case "email":
      return <EmailInput {...common} />;
    case "tel":
      return <PhoneInput {...common} />;
    case "url":
      return <LinkInput {...common} placeholder={field.placeholder} />;
    case "month":
      return <MonthYearPicker {...common} label={field.label} />;
    case "textarea":
      return <AutoTextarea {...common} maxLength={field.maxLength} />;
    case "select":
      return (
        <SelectField
          {...common}
          options={field.options ?? []}
          icon={field.icon}
          placeholder={field.placeholder}
        />
      );
    case "proficiency":
      return <ProficiencyField {...common} label={field.label} />;
    default:
      return (
        <IconInput
          {...common}
          icon={field.icon}
          placeholder={field.placeholder}
          maxLength={field.maxLength}
        />
      );
  }
}

export function ReviewDraftFields({
  fields,
  values,
  onChange,
}: {
  fields: readonly ReviewField[];
  values: DraftFields;
  onChange: (values: DraftFields) => void;
}) {
  const id = useId();

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {fields.map((field) => {
        const fieldId = `${id}-${field.key}`;

        if (field.type === "period" && field.period) {
          return (
            <PeriodField
              key={field.key}
              id={fieldId}
              period={field.period}
              values={values}
              onChange={onChange}
            />
          );
        }

        return (
          <FieldFrame
            key={field.key}
            id={fieldId}
            label={field.label}
            hint={field.hint}
            wide={field.wide}
          >
            {renderControl(field, fieldId, values[field.key] ?? "", (next) =>
              onChange({ ...values, [field.key]: next }),
            )}
          </FieldFrame>
        );
      })}
    </div>
  );
}
