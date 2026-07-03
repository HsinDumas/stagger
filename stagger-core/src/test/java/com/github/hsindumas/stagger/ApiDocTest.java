package com.github.hsindumas.stagger;

import com.github.hsindumas.stagger.builder.ApiDocBuilder;
import com.github.hsindumas.stagger.builder.HtmlApiDocBuilder;
import com.github.hsindumas.stagger.builder.JMeterBuilder;
import com.github.hsindumas.stagger.builder.openapi.OpenApiBuilder;
import com.github.hsindumas.stagger.builder.openapi.SwaggerBuilder;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.constants.SpringMvcAnnotations;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.SourceCodePath;
import com.github.hsindumas.stagger.model.annotation.FrameworkAnnotations;
import com.github.hsindumas.stagger.model.annotation.MappingAnnotation;
import com.github.hsindumas.stagger.template.SpringBootDocBuildTemplate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Description: ApiDoc Test
 *
 * @author yu 2018/06/11.
 * @author HsinDumas
 */
public class ApiDocTest {

	@TempDir
	Path tempDir;

	/**
	 * test html
	 * @throws IOException io exception
	 */
	@Test
	public void testBuilderControllersApi() throws IOException {
		Path projectRoot = this.createSpringFixtureProject("html-fixture");
		Path outPath = projectRoot.resolve("docs-html");
		ApiConfig config = this.newBaseSpringConfig(projectRoot, outPath);
		config.setAllInOneDocFileName("fixture-index.html");

		HtmlApiDocBuilder.buildApiDoc(config);

		assertTrue(Files.exists(outPath.resolve("fixture-index.html")), "HTML doc should be generated");
	}

	/**
	 * test jmeter
	 * @throws IOException io exception
	 */
	@Test
	public void testJmxBuilderControllersApi() throws IOException {
		Path projectRoot = this.createSpringFixtureProject("jmeter-fixture");
		Path outPath = projectRoot.resolve("docs-jmeter");
		ApiConfig config = this.newBaseSpringConfig(projectRoot, outPath);
		config.setProjectName("fixture-jmeter");

		JMeterBuilder.buildApiDoc(config);

		assertTrue(Files.exists(outPath.resolve("fixture-jmeter.jmx")), "JMeter script should be generated");
	}

	/**
	 * test markdown
	 * @throws IOException io exception
	 */
	@Test
	public void testMdBuilderControllersApi1() throws IOException {
		Path projectRoot = this.createSpringFixtureProject("markdown-fixture");
		Path outPath = projectRoot.resolve("docs-md");
		ApiConfig config = this.newBaseSpringConfig(projectRoot, outPath);
		config.setAllInOneDocFileName("fixture-allinone");

		ApiDocBuilder.buildApiDoc(config);

		Path markdownFile = outPath.resolve("fixture-allinone.md");
		assertTrue(Files.exists(markdownFile), "Markdown doc should be generated");
		assertTrue(Files.size(markdownFile) > 0, "Markdown doc should not be empty");
		String markdown = Files.readString(markdownFile, StandardCharsets.UTF_8);
		String fixtureLines = markdown.lines()
			.filter(line -> line.contains("fixture"))
			.reduce((left, right) -> left + "\n" + right)
			.orElse("<none>");
		assertTrue(markdown.contains("/fixture/exchange/{id}"),
				"GetExchange endpoint should appear in markdown output. fixture lines:\n" + fixtureLines);
		assertTrue(markdown.contains("X-Trace-Id"),
				"RequestHeader from web.service annotation should appear in markdown output");
		assertTrue(markdown.contains("name"),
				"RequestParam from web.service annotation should appear in markdown output");
	}

	@Test
	public void testOpenApiAndSwaggerGenerateDifferentFiles() throws IOException {
		Path projectRoot = this.createSpringFixtureProject("openapi-swagger-fixture");
		Path outPath = projectRoot.resolve("docs-openapi-swagger");
		ApiConfig config = this.newBaseSpringConfig(projectRoot, outPath);

		OpenApiBuilder.buildOpenApi(config);

		Path openApiFile = outPath.resolve("openapi.json");
		assertTrue(Files.exists(openApiFile), "openapi.json should be generated");
		String openApiContent = Files.readString(openApiFile, StandardCharsets.UTF_8);
		assertTrue(openApiContent.contains("\"openapi\": \"3.1.0\""), "openapi.json should contain OpenAPI 3.1 marker");

		SwaggerBuilder.buildOpenApi(config);

		Path swaggerFile = outPath.resolve("swagger.json");
		assertTrue(Files.exists(swaggerFile), "swagger.json should be generated");
		String swaggerContent = Files.readString(swaggerFile, StandardCharsets.UTF_8);
		assertTrue(swaggerContent.contains("\"swagger\": \"2.0\""), "swagger.json should contain Swagger 2.0 marker");

		String openApiContentAfterSwagger = Files.readString(openApiFile, StandardCharsets.UTF_8);
		assertTrue(openApiContentAfterSwagger.contains("\"openapi\": \"3.1.0\""),
				"openapi.json should not be overwritten by swagger generation");
	}

	@Test
	public void testBoot4ExchangeMappingsRegistered() {
		FrameworkAnnotations annotations = new SpringBootDocBuildTemplate().registeredAnnotations();
		Map<String, MappingAnnotation> mappingAnnotations = annotations.getMappingAnnotations();
		assertTrue(mappingAnnotations.containsKey(SpringMvcAnnotations.GET_EXCHANGE),
				"GetExchange mapping should be registered");
		assertTrue(mappingAnnotations.containsKey(SpringMvcAnnotations.GET_EXCHANGE_FULLY),
				"Fully-qualified GetExchange mapping should be registered");
		assertTrue(mappingAnnotations.containsKey(SpringMvcAnnotations.HTTP_EXCHANGE_FULLY),
				"Fully-qualified HttpExchange mapping should be registered");
		assertEquals("GET", mappingAnnotations.get(SpringMvcAnnotations.GET_EXCHANGE).getMethodType(),
				"GetExchange should be mapped to GET");
	}

	private ApiConfig newBaseSpringConfig(Path projectRoot, Path outPath) {
		ApiConfig config = new ApiConfig();
		config.setFramework(FrameworkEnum.SPRING.getFramework());
		config.setServerUrl("http://127.0.0.1:8080");
		config.setAllInOne(true);
		config.setCoverOld(true);
		config.setCreateDebugPage(false);
		config.setOutPath(outPath.toString());
		config.setPackageFilters("sample.controller");
		config.setBaseDir(projectRoot.toString());
		config.setCodePath("src/main/java");
		config.setSourceCodePaths(SourceCodePath.builder()
			.setDesc("in-repo-fixture")
			.setPath(projectRoot.resolve("src/main/java").toString()));
		return config;
	}

	private Path createSpringFixtureProject(String folderName) throws IOException {
		Path projectRoot = tempDir.resolve(folderName);
		Path sourceRoot = projectRoot.resolve("src/main/java/sample/controller");
		Files.createDirectories(sourceRoot);
		String controllerSource = "package sample.controller;\n\n"
				+ "import org.springframework.web.bind.annotation.GetMapping;\n"
				+ "import org.springframework.web.bind.annotation.PathVariable;\n"
				+ "import org.springframework.web.bind.annotation.RestController;\n"
				+ "import org.springframework.web.service.annotation.GetExchange;\n"
				+ "import org.springframework.web.service.annotation.RequestHeader;\n"
				+ "import org.springframework.web.service.annotation.RequestParam;\n\n" + "@RestController\n"
				+ "public class FixtureController {\n\n" + "    @GetMapping(\"/fixture/hello/{id}\")\n"
				+ "    public String hello(\n" + "            @PathVariable(\"id\") String id,\n"
				+ "            @RequestParam(\"name\") String name,\n"
				+ "            @RequestHeader(\"X-Trace-Id\") String traceId) {\n"
				+ "        return \"hello-\" + name + \"-\" + traceId + \"-\" + id;\n" + "    }\n\n"
				+ "    @GetExchange(url = \"/fixture/exchange/{id}\")\n" + "    public String exchange(\n"
				+ "            @org.springframework.web.service.annotation.PathVariable(\"id\") String id,\n"
				+ "            @RequestParam(\"name\") String name) {\n" + "        return id + \"-\" + name;\n"
				+ "    }\n" + "}\n";
		Files.writeString(sourceRoot.resolve("FixtureController.java"), controllerSource, StandardCharsets.UTF_8);
		return projectRoot;
	}

}
