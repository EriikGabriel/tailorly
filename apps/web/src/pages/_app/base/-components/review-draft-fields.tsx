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
import { PENDING_RING } from "../-constants/pending";
import { usePendingFields } from "../-hooks/use-pending-fields";
import { PendingNote } from "./pending-note";
import { VerifiedMark } from "./verified-mark";

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
  scope,
}: {
  fields: readonly ReviewField[];
  values: DraftFields;
  onChange: (values: DraftFields) => void;
  /** "identity" ou o id da linha da coleção; liga o campo às pendências do fato. */
  scope: string;
}) {
  const id = useId();
  const pending = usePendingFields();

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {fields.map((field) => {
        const fieldId = `${id}-${field.key}`;

        if (field.type === "period" && field.period) {
          const issues = [
            field.period.startKey,
            field.period.endKey,
            field.period.statusKey,
          ]
            .filter((key): key is string => Boolean(key))
            .map((key) => pending.field(scope, key))
            .filter((issue) => issue !== undefined);
          const periodKeys = [
            field.period.startKey,
            field.period.endKey,
            field.period.statusKey,
          ].filter((key): key is string => Boolean(key));
          const periodVerified =
            issues.length === 0 &&
            periodKeys.some((key) => pending.verified(scope, key));
          return (
            <div
              key={field.key}
              className={`space-y-1.5 sm:col-span-2 ${issues.length > 0 ? PENDING_RING : ""}`}
            >
              <PeriodField
                id={fieldId}
                period={field.period}
                values={values}
                onChange={onChange}
              />
              {periodVerified && (
                <VerifiedMark showLabel label="Período verificado" />
              )}
              {issues.map((issue) => (
                <div key={issue.id} className="space-y-0.5">
                  <p className="text-[11px] text-on-surface-variant">
                    {issue.fieldLabel}: {issue.value}
                  </p>
                  <PendingNote issue={issue} />
                </div>
              ))}
            </div>
          );
        }

        const issue = pending.field(scope, field.key);

        return (
          <FieldFrame
            key={field.key}
            id={fieldId}
            label={field.label}
            hint={field.hint}
            wide={field.wide}
            highlight={issue ? PENDING_RING : undefined}
            verified={!issue && pending.verified(scope, field.key)}
            status={issue ? <PendingNote issue={issue} /> : undefined}
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
