package com.tailorly.api.user.retention;

import com.tailorly.api.user.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Beans da retenção de dados de contas. Em {@code @Configuration} pelo mesmo motivo de
 * {@code com.tailorly.api.session.AnonymousSessionConfiguration}: testes de fatia web não
 * precisam carregá-los. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(AccountRetentionProperties.class)
public class AccountRetentionConfiguration {

  @Bean
  @ConditionalOnProperty(name = "tailorly.account.cleanup-enabled", havingValue = "true",
      matchIfMissing = true)
  public AccountRetentionJob accountRetentionJob(UserRepository users,
      AccountDataRetentionService retention, AccountRetentionProperties properties) {
    return new AccountRetentionJob(users, retention, properties);
  }
}
