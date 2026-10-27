package com.tailorly.api.shared.config;

import java.lang.reflect.Type;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

/**
 * Mapeador JSON do Hibernate que sempre serializa via Jackson.
 *
 * <p>O mapeador padrão trata valores {@code String}/{@code Object} como texto que já é JSON e os
 * grava sem aspas, o que quebra colunas {@code jsonb} quando o valor é um texto simples (por
 * exemplo, um telefone como {@code (11) 98765-4321} extraído pela IA). Aqui um {@code String}
 * vira um literal JSON e é lido de volta como {@code String}.</p>
 */
public class JacksonJsonValueFormatMapper implements FormatMapper {
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Override
  public <T> String toString(T value, JavaType<T> javaType, WrapperOptions options) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Could not serialize value as JSON", exception);
    }
  }

  @Override
  public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions options) {
    Type type = javaType.getJavaType();
    try {
      return objectMapper.readValue(charSequence.toString(), objectMapper.constructType(type));
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("Could not deserialize JSON as " + type, exception);
    }
  }
}
