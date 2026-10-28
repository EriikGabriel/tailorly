package com.tailorly.api.extraction.interpreting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class FactValueNormalizerTest {
  private static final UUID PAGE = UUID.randomUUID();

  private static String value(String category, String key, Object raw) {
    return FactValueNormalizer.normalizeValue(category, key, raw);
  }

  private static FactProposal fact(String category, String key, Object value, String excerpt) {
    return new FactProposal(category, key, value, PAGE, excerpt, 0, excerpt.length());
  }

  private static List<String> keysAndValues(List<FactProposal> proposals) {
    return proposals.stream().map(p -> p.category() + ":" + p.fieldKey() + "=" + p.extractedValue()).toList();
  }

  @Test
  void datesBecomeYearMonthOrYear() {
    for (String input : List.of("2022-01", "2022-01-15", "2022-1", "01/2022", "1/2022", "jan/2022",
        "Jan 2022", "janeiro de 2022", "January 2022", "jan. 2022", "15/01/2022", "2022/01", "15 de janeiro de 2022")) {
      assertEquals("2022-01", value("experience", "start", input), input);
    }
    assertEquals("2021-09", value("experience", "end", "set/2021"));
    assertEquals("2021-09", value("experience", "end", "Sept 2021"));
    assertEquals("2021-03", value("certifications", "issued", "Março de 2021"));
    assertEquals("2015", value("degrees", "start", "2015"));
    assertEquals("2015", value("degrees", "start", 2015));
  }

  @Test
  void unrecognizedOrInvalidDatesAreKeptAsWritten() {
    assertEquals("primeiro semestre de 2019", value("experience", "start", "primeiro semestre de 2019"));
    assertEquals("2022-13", value("experience", "start", "2022-13"));
    assertEquals("13/2022", value("experience", "start", "13/2022"));
  }

  @Test
  void statusUsesTheCanonicalWordsOfEachCategory() {
    assertEquals("Atual", value("experience", "status", "current"));
    assertEquals("Atual", value("experience", "status", "Presente"));
    assertEquals("Encerrado", value("experience", "status", "finalizado"));
    assertEquals("Em andamento", value("degrees", "status", "cursando"));
    assertEquals("Em andamento", value("degrees", "status", "Atual"));
    assertEquals("Concluído", value("degrees", "status", "concluido"));
    assertEquals("Concluído", value("degrees", "status", "Completed"));
    // Texto próprio do documento não é adivinhado.
    assertEquals("Trancado", value("degrees", "status", "Trancado"));
  }

  @Test
  void employmentMapsKnownContractTypes() {
    assertEquals("CLT", value("experience", "employment", "clt"));
    assertEquals("CLT", value("experience", "employment", "Contratado (CLT)"));
    assertEquals("PJ", value("experience", "employment", "Pessoa Jurídica"));
    assertEquals("Estágio", value("experience", "employment", "Intern"));
    assertEquals("Estágio", value("experience", "employment", "Estagiário"));
    assertEquals("Autônomo / Freelancer", value("experience", "employment", "freelance"));
    assertEquals("Temporário", value("experience", "employment", "Temporary"));
    // Voluntariado não é um tipo de vínculo empregatício: fica como escrito (a categoria própria trata disso).
    assertEquals("Voluntário", value("experience", "employment", "Voluntário"));
    // Palavras parecidas não são confundidas.
    assertEquals("Tempo integral", value("experience", "employment", "Tempo integral"));
    assertEquals("International", value("experience", "employment", "International"));
  }

  @Test
  void proficiencyMapsLevelsAndKeepsCodes() {
    assertEquals("Avançado", value("languages", "proficiency", "avançado"));
    assertEquals("Avançado", value("languages", "proficiency", "Advanced"));
    assertEquals("Nativo", value("languages", "proficiency", "Nativo ou bilíngue"));
    assertEquals("Fluente", value("languages", "proficiency", "fluent"));
    assertEquals("Intermediário", value("languages", "proficiency", "intermediate"));
    assertEquals("Básico", value("languages", "proficiency", "básico"));
    assertEquals("C1", value("languages", "proficiency", "C1"));
    assertEquals("TOEFL 100", value("languages", "proficiency", "TOEFL 100"));
  }

  @Test
  void brazilianPhonesAreMaskedAndOthersKept() {
    assertEquals("(11) 98765-4321", value("identity", "phone", "11987654321"));
    assertEquals("(11) 98765-4321", value("identity", "phone", "(11) 98765-4321"));
    assertEquals("(11) 3333-4444", value("identity", "phone", "11 3333 4444"));
    assertEquals("+55 (11) 98765-4321", value("identity", "phone", "+55 11 98765-4321"));
    assertEquals("+55 (11) 98765-4321", value("identity", "phone", "5511987654321"));
    assertEquals("+1 415 555 0100", value("identity", "phone", "+1  415 555 0100"));
    assertEquals("12345", value("identity", "phone", "12345"));
  }

  @Test
  void linksAndEmailsLoseWrappingPunctuation() {
    assertEquals("github.com/EriikGabriel", value("identity", "github", "github.com/EriikGabriel/"));
    assertEquals("https://www.linkedin.com/in/pessoa", value("identity", "linkedin", "<https://www.linkedin.com/in/pessoa/>."));
    assertEquals("erikgabriel.vercel.app", value("identity", "website", "erikgabriel.vercel.app,"));
    assertEquals("github.com/EriikGabriel/ufscar-planner", value("projects", "link", "github.com/EriikGabriel/ufscar-planner/"));
    assertEquals("erik@example.com", value("identity", "email", "mailto:erik@example.com"));
    assertEquals("erik@example.com", value("identity", "email", "<erik@example.com>"));
  }

  @Test
  void textIsTidiedAndMultilineFieldsKeepTheirLineBreaks() {
    assertEquals("Erik Gabriel", value("identity", "name", "  Erik   Gabriel  "));
    assertEquals("Liderança de squads\nRedução de latência",
        value("experience", "description", "Liderança   de squads \n  Redução de latência  "));
  }

  @Test
  void unusableValuesAreRejected() {
    assertNull(value("identity", "name", ""));
    assertNull(value("identity", "name", "   "));
    assertNull(value("identity", "name", null));
    assertNull(value("identity", "name", true));
    assertNull(value("identity", "name", java.util.Map.of("a", "b")));
    assertEquals("Java, Spring", value("technologies", "name", List.of("Java", "Spring")));
    assertEquals("35", value("projects", "metric", 35.0));
  }

  @Test
  void aCurrentEndBecomesTheItemStatus() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("experience", "0:start", "jan/2022", "jan/2022"),
        fact("experience", "0:end", "Atual", "Atual"),
        fact("degrees", "0:end", "em andamento", "em andamento")));

    assertEquals(List.of(
        "experience:0:start=2022-01",
        "experience:0:status=Atual",
        "degrees:0:status=Em andamento"), keysAndValues(result));
    // A evidência continua sendo o texto original do documento.
    assertEquals("Atual", result.get(1).sourceExcerpt());
  }

  @Test
  void anExistingStatusIsNotDuplicatedByTheCurrentEnd() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("experience", "0:status", "current", "Atual"),
        fact("experience", "0:end", "Presente", "Presente")));

    assertEquals(List.of("experience:0:status=Atual"), keysAndValues(result));
  }

  @Test
  void aRealEndDateIsKept() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("experience", "1:end", "dez/2021", "dez/2021")));

    assertEquals(List.of("experience:1:end=2021-12"), keysAndValues(result));
  }

  @Test
  void identicalFactsAndRepeatedItemsAreDropped() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("identity", "email", "erik@example.com", "erik@example.com"),
        fact("identity", "email", "mailto:erik@example.com", "erik@example.com"),
        fact("technologies", "0:name", "Java", "Java"),
        fact("technologies", "0:context", "Backend", "Backend"),
        fact("technologies", "1:name", "java", "Java"),
        fact("technologies", "1:context", "Outro contexto", "Outro contexto"),
        fact("technologies", "2:name", "React", "React"),
        fact("languages", "0:language", "Inglês", "Inglês"),
        fact("languages", "1:language", "inglês", "inglês")));

    assertEquals(List.of(
        "identity:email=erik@example.com",
        "technologies:0:name=Java",
        "technologies:0:context=Backend",
        "technologies:2:name=React",
        "languages:0:language=Inglês"), keysAndValues(result));
  }

  @Test
  void proposalsWithoutAUsableValueAreRemoved() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("identity", "name", "   ", "x"),
        fact("identity", "headline", java.util.Map.of("k", "v"), "x"),
        fact("identity", "location", "São Paulo, SP", "São Paulo, SP")));

    assertEquals(List.of("identity:location=São Paulo, SP"), keysAndValues(result));
  }

  @Test
  void volunteerWorkMarkedAsExperienceMovesToItsOwnCategory() {
    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("experience", "0:company", "FinTech Solutions Brasil", "FinTech Solutions Brasil"),
        fact("experience", "0:role", "Tech Lead", "Tech Lead"),
        fact("experience", "1:company", "ONG Educar", "ONG Educar"),
        fact("experience", "1:role", "Instrutor", "Instrutor"),
        fact("experience", "1:employment", "Trabalho voluntário", "Trabalho voluntário"),
        fact("experience", "1:start", "mar/2020", "mar/2020"),
        fact("experience", "1:end", "Atual", "Atual"),
        fact("volunteering", "0:organization", "Projeto Casa", "Projeto Casa")));

    assertEquals(List.of(
        "experience:0:company=FinTech Solutions Brasil",
        "experience:0:role=Tech Lead",
        "volunteering:1:organization=ONG Educar",
        "volunteering:1:role=Instrutor",
        "volunteering:1:start=2020-03",
        "volunteering:1:status=Atual",
        "volunteering:0:organization=Projeto Casa"), keysAndValues(result));
  }

  @Test
  void volunteeringUsesTheExperienceStatusWords() {
    assertEquals("Atual", value("volunteering", "status", "current"));
    assertEquals("Encerrado", value("volunteering", "status", "finalizado"));

    List<FactProposal> result = FactValueNormalizer.normalize(List.of(
        fact("volunteering", "0:end", "Presente", "Presente")));
    assertEquals(List.of("volunteering:0:status=Atual"), keysAndValues(result));
  }
}
