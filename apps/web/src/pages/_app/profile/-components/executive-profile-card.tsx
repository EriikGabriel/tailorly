import { CountingNumber } from "@animate/primitives/texts/counting-number";
import {
  Crown,
  Github,
  Linkedin,
  MapPin,
  Pencil,
} from "@react-zero-ui/icon-sprite";
import { previewExecutiveProfile } from "@/data/profile-preview";
import { initials } from "@/utils/review-format";

export function ExecutiveProfileCard() {
  const profile = previewExecutiveProfile;

  return (
    <div className="flex w-full flex-col gap-4 rounded-xl bg-card p-6 shadow-xs">
      <div className="flex items-start justify-between gap-3">
        <div className="relative shrink-0">
          <div className="flex size-20 items-center justify-center rounded-xl bg-surface-container text-headline-sm font-bold text-primary-950 shadow-xs">
            {initials(profile.name)}
          </div>
          <span className="absolute -right-1 -bottom-1 flex size-6 items-center justify-center rounded-full bg-primary text-primary-foreground shadow-xs">
            <Pencil aria-hidden="true" className="size-3" />
          </span>
        </div>
        <span className="inline-flex shrink-0 items-center gap-1.5 rounded-full bg-secondary-container px-3 py-1 text-label-sm text-on-secondary-container">
          <Crown aria-hidden="true" className="size-3" />
          {profile.plan}
        </span>
      </div>

      <div className="flex flex-col gap-1">
        <h2 className="text-headline-sm font-semibold text-primary-950">
          {profile.name}
        </h2>
        <p className="text-label-md font-medium text-on-secondary-container">
          {profile.role}
        </p>
        <p className="flex items-center gap-1 pt-1 text-body-sm text-on-surface-variant">
          <MapPin aria-hidden="true" className="size-3.5 shrink-0" />
          {profile.location}
        </p>
      </div>

      <div className="flex flex-wrap items-center gap-2 pt-1">
        <a
          href={`https://${profile.linkedin}`}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1.5 rounded-lg bg-surface-container px-3 py-1.5 text-label-sm font-semibold text-primary-container hover:bg-surface-container-high"
        >
          <Linkedin aria-hidden="true" className="size-3.5" />
          {profile.linkedin}
        </a>
        <a
          href={`https://${profile.github}`}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1.5 rounded-lg bg-surface-container px-3 py-1.5 text-label-sm font-semibold text-primary-container hover:bg-surface-container-high"
        >
          <Github aria-hidden="true" className="size-3.5" />
          {profile.github}
        </a>
      </div>

      <div className="grid grid-cols-3 gap-2 rounded-lg bg-surface-container-low p-2">
        <Stat value={profile.stats.resumes} label="Currículos" />
        <Stat
          value={profile.stats.atsScore}
          decimalPlaces={1}
          suffix="%"
          accent
          label="ATS Score"
        />
        <Stat value={profile.stats.exports} label="Exportados" />
      </div>
    </div>
  );
}

function Stat({
  value,
  label,
  suffix = "",
  decimalPlaces = 0,
  accent = false,
}: {
  value: number;
  label: string;
  suffix?: string;
  decimalPlaces?: number;
  accent?: boolean;
}) {
  return (
    <div className="flex flex-col items-center">
      <span
        className={`text-headline-sm font-bold ${accent ? "text-on-tertiary-container" : "text-primary-950"}`}
      >
        <CountingNumber
          number={value}
          decimalPlaces={decimalPlaces}
          inView
          inViewOnce
        />
        {suffix}
      </span>
      <span className="text-label-sm text-on-surface-variant">{label}</span>
    </div>
  );
}
