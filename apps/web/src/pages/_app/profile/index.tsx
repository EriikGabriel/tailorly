import { ApiClientError } from "@lib/api/client";
import { currentAccountQueryOptions } from "@queries/current-account";
import { useAuthStore } from "@stores/auth-store";
import { resetClientState } from "@stores/reset-client-state";
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
  beforeLoad: async ({ context }) => {
    const accessToken = useAuthStore.getState().accessToken;
    if (!accessToken) {
      throw redirect({ to: "/" });
    }

    // Ter token não basta: a conta pode ter sido removida (ex.: banco recriado).
    try {
      await context.queryClient.fetchQuery(
        currentAccountQueryOptions(accessToken),
      );
    } catch (error) {
      if (
        error instanceof ApiClientError &&
        (error.status === 401 || error.status === 404)
      ) {
        resetClientState();
        context.queryClient.removeQueries();
        throw redirect({ to: "/" });
      }
      throw error;
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
