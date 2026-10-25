import { useAuthStore } from "@stores/auth-store";
import { createFileRoute, redirect } from "@tanstack/react-router";
import { AiPreferencesSection } from "./-components/ai-preferences-section";
import { GroundTruthPrincipalSection } from "./-components/ground-truth-principal-section";
import { IdentitySection } from "./-components/identity-section";
import { ProfilePageHeader } from "./-components/profile-page-header";
import { ProfileSaveBar } from "./-components/profile-save-bar";
import { ProfileSidebar } from "./-components/profile-sidebar";
import { ProfileTabs } from "./-components/profile-tabs";
import { SecondBrainIntegrationsSection } from "./-components/second-brain-integrations-section";
export const Route = createFileRoute("/_app/profile/")({
  beforeLoad: () => {
    if (!useAuthStore.getState().accessToken) {
      throw redirect({ to: "/" });
    }
  },
  component: ProfilePage,
});

function ProfilePage() {
  return (
    <main className="min-h-[calc(100dvh-4rem)] px-4 py-6 sm:px-8 lg:px-16">
      <div className="mx-auto flex w-full max-w-384 flex-col gap-6">
        <ProfilePageHeader />
        <ProfileTabs />

        <div className="grid gap-6 lg:grid-cols-12">
          <ProfileSidebar />

          <div className="flex flex-col gap-6 lg:col-span-8">
            <div id="section-overview" className="scroll-mt-16">
              <IdentitySection />
            </div>
            <div id="section-ground-truth" className="scroll-mt-16">
              <GroundTruthPrincipalSection />
            </div>
            <div id="section-connections" className="scroll-mt-16">
              <SecondBrainIntegrationsSection />
            </div>
            <div id="section-ai-preferences" className="scroll-mt-16">
              <AiPreferencesSection />
            </div>
          </div>
        </div>

        <ProfileSaveBar />
      </div>
    </main>
  );
}
