import {
  BookOpenText,
  Briefcase,
  CalendarDays,
  ChartColumnIncreasing,
  Check,
  CircleUserRound,
  EyeOff,
  FolderBookmark,
  GraduationCap,
  HandHeart,
  IconClock,
  type IconProps,
  IconQuote,
  IdCard,
  Languages,
  List,
  ListOrdered,
  Mail,
  Text,
} from "@react-zero-ui/icon-sprite";
import type { ComponentType } from "react";
import type {
  CorrectionField,
  ReviewCategory,
  ReviewChoice,
  ReviewIssue,
} from "../types/review";

/** Ícones e rótulos usados para apresentar as pendências da revisão rápida. */

const correctionIcons = {
  text: Text,
  email: Mail,
  month: CalendarDays,
  number: ListOrdered,
  select: List,
} satisfies Record<CorrectionField["type"], typeof Text>;

/** Ícone do botão de resposta, pelo resultado da escolha (confirmar, omitir ou corrigir). */
export function getChoiceIcon(choice: ReviewChoice) {
  switch (choice.result) {
    case "confirmed":
      return Check;
    case "omitted":
      return EyeOff;
    case "corrected":
      return correctionIcons[choice.correction.type];
  }
}

export const issueTypeMap: Record<
  ReviewIssue["type"],
  {
    label: string;
    icon: ComponentType<IconProps>;
  }
> = {
  metric: {
    label: "Métrica relevante para esta vaga",
    icon: ChartColumnIncreasing,
  },
  temporal: {
    label: "Dúvida Temporal",
    icon: IconClock,
  },
  fact: {
    label: "Dado extraído do seu currículo",
    icon: IconQuote,
  },
};

export const categoryIconMap: Record<
  ReviewCategory,
  ComponentType<IconProps>
> = {
  education: GraduationCap,
  identity: IdCard,
  employment: Briefcase,
  volunteering: HandHeart,
  projects: FolderBookmark,
  skills: BookOpenText,
  languages: Languages,
  presentation: CircleUserRound,
};
