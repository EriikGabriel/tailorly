package com.tailorly.api.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hibernate.type.descriptor.java.StringJavaType;
import org.junit.jupiter.api.Test;

class JacksonJsonValueFormatMapperTest {
  private final JacksonJsonValueFormatMapper mapper = new JacksonJsonValueFormatMapper();

  @Test
  void serializesPlainTextAsJsonStringLiteral() {
    assertEquals("\"(11) 98765-4321\"", mapper.toString("(11) 98765-4321", StringJavaType.INSTANCE, null));
  }

  @Test
  void readsJsonStringLiteralBackAsPlainText() {
    assertEquals("(11) 98765-4321", mapper.fromString("\"(11) 98765-4321\"", StringJavaType.INSTANCE, null));
  }
}
