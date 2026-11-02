package com.tailorly.api.diligence;

import java.util.List;

/**
 * Os onze fatores da due diligence. A lista é fechada e todo relatório traz os onze, mesmo sem dados:
 * o {@code id} é o identificador gravado e exibido pela interface; {@code description} orienta o modelo e
 * {@code queries} são as buscas públicas ({@code %s} = nome da empresa).
 */
public enum DiligenceFactor {
  LABOR_ISSUES("labor_issues", "labor lawsuits, labor ministry inspections, wage or working-condition violations",
      "\"%s\" problemas trabalhistas ações trabalhistas fiscalização"),
  FORMER_EMPLOYEE_REPORTS("former_employee_reports", "accounts from former employees (reviews, forums)",
      "\"%s\" avaliações ex-funcionários Glassdoor"),
  CULTURE("culture", "work culture, values, management style, diversity, work model",
      "\"%s\" cultura da empresa ambiente de trabalho"),
  FINANCIAL_HEALTH("financial_health", "revenue, profit, layoffs, funding, insolvency, judicial recovery",
      "\"%s\" situação financeira resultado demissões recuperação judicial"),
  ONLINE_REPUTATION("online_reputation", "public image, press coverage, consumer complaints",
      "\"%s\" reputação reclamações Reclame Aqui"),
  LEGAL_PROCEEDINGS("legal_proceedings", "lawsuits, fines, sanctions, investigations involving the company",
      "\"%s\" processos judiciais multa investigação"),
  SUPPLIERS("suppliers", "key suppliers and supply-chain relationships or disputes",
      "\"%s\" fornecedores cadeia de suprimentos"),
  CUSTOMERS("customers", "main customers, market segments and customer relationships",
      "\"%s\" clientes principais cases"),
  REGULATORS("regulators", "relationship with regulators, licenses, regulatory penalties",
      "\"%s\" regulador órgão regulador licença penalidade"),
  INVESTORS("investors", "shareholders, investors, funding rounds, ownership",
      "\"%s\" investidores rodada de investimento acionistas"),
  PARTNERS("partners", "commercial, technology or institutional partnerships",
      "\"%s\" parceiros parceria");

  private final String id;
  private final String description;
  private final String queryTemplate;

  DiligenceFactor(String id, String description, String queryTemplate) {
    this.id = id;
    this.description = description;
    this.queryTemplate = queryTemplate;
  }

  public String id() {
    return id;
  }

  public String description() {
    return description;
  }

  public String query(String company) {
    return queryTemplate.formatted(company);
  }

  public static List<DiligenceFactor> all() {
    return List.of(values());
  }

  public static DiligenceFactor fromId(String id) {
    for (DiligenceFactor factor : values())
      if (factor.id.equals(id))
        return factor;
    return null;
  }
}
