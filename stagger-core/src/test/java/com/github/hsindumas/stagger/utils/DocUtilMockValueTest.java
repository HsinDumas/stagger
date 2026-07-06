package com.github.hsindumas.stagger.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hsindumas.stagger.constants.DocGlobalConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @author HsinDumas
 */
class DocUtilMockValueTest {

    private static final String MOCK_FIELD_VALUES_PATH = "stagger.mock.field.values.path";

    @AfterEach
    void tearDown() {
        System.clearProperty(DocGlobalConstants.RANDOM_MOCK);
        System.clearProperty(MOCK_FIELD_VALUES_PATH);
    }

    @Test
    void shouldGenerateEmailFromEmailConstraint() {
        System.setProperty(DocGlobalConstants.RANDOM_MOCK, "true");
        List<?> annotations = List.of(new FakeAnnotation("Email", Map.of()));
        String value = DocUtil.getValByTypeAndFieldName("String", "contact", annotations, true);
        assertTrue(value.contains("@"));
    }

    @Test
    void shouldGenerateValueMatchingPatternConstraint() {
        System.setProperty(DocGlobalConstants.RANDOM_MOCK, "true");
        List<?> annotations = List.of(new FakeAnnotation("Pattern", Map.of("regexp", "[A-Z]{3}[0-9]{2}")));
        String value = DocUtil.getValByTypeAndFieldName("String", "code", annotations, true);
        assertTrue(value.matches("[A-Z]{3}[0-9]{2}"));
    }

    @Test
    void shouldGenerateValueInNumericRange() {
        System.setProperty(DocGlobalConstants.RANDOM_MOCK, "true");
        List<?> annotations = List.of(
                new FakeAnnotation("Min", Map.of("value", "18")), new FakeAnnotation("Max", Map.of("value", "65")));
        int value = Integer.parseInt(DocUtil.getValByTypeAndFieldName("int", "age", annotations, true));
        assertTrue(value >= 18 && value <= 65);
    }

    @Test
    void shouldAllowOverridingFieldDictionaryFromExternalProperties() throws IOException {
        System.setProperty(DocGlobalConstants.RANDOM_MOCK, "true");
        Path override = Files.createTempFile("stagger-mock-values", ".properties");
        Files.writeString(override, "email-string=override@example.com\n", StandardCharsets.UTF_8);
        System.setProperty(MOCK_FIELD_VALUES_PATH, override.toString());

        String value = DocUtil.getValByTypeAndFieldName("String", "email", true);
        assertTrue("override@example.com".equals(value));
    }

    private static final class FakeAnnotation {

        private final FakeAnnotationType type;

        private final Map<String, Object> properties;

        private FakeAnnotation(String name, Map<String, String> properties) {
            this.type = new FakeAnnotationType(name);
            this.properties = new HashMap<>(properties);
        }

        public FakeAnnotationType getType() {
            return type;
        }

        public Object getProperty(String propertyName) {
            return properties.get(propertyName);
        }

        public Map<String, Object> getPropertyMap() {
            return properties;
        }
    }

    private static final class FakeAnnotationType {

        private final String value;

        private FakeAnnotationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public String getSimpleName() {
            return value;
        }

        public String getFullyQualifiedName() {
            return value;
        }
    }
}
