package com.github.hsindumas.stagger.qbox;

import com.github.hsindumas.stagger.builder.HtmlApiDocBuilder;
import com.github.hsindumas.stagger.constants.FrameworkEnum;
import com.github.hsindumas.stagger.model.ApiConfig;
import com.github.hsindumas.stagger.model.SourceCodePath;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * stagger
 *
 * @author spencer
 * @author HsinDumas
 * @project stagger
 * @date 2022-01-2022/1/13
 */
public class QboxScanSourceTest {

    @Test
    public void scanError() {
        // target source folder for scan
        String testJavaDirectory =
                Paths.get("src", "test", "java").toAbsolutePath().toString();
        String outPath = Paths.get("target").toAbsolutePath().toString();

        // config and scan
        ApiConfig config = new ApiConfig();
        config.setServerUrl("HSF://127.0.0.1:8088");
        config.setDebugEnvName("Test environment");
        config.setStyle("randomLight");
        config.setCreateDebugPage(true);
        config.setDebugEnvUrl("HSF://127.0.0.1");
        config.setCreateDebugPage(false);
        config.setAllInOne(true);
        config.setOutPath(outPath);
        config.setMd5EncryptedHtmlName(true);
        config.setFramework(FrameworkEnum.DUBBO.getFramework());
        config.setSourceCodePaths(
                SourceCodePath.builder().setDesc("tesSourceScan").setPath(testJavaDirectory));
        config.setBaseDir(Paths.get(".").toAbsolutePath().normalize().toString());
        config.setCodePath("/src/test/java");

        // This bug caused not all source code to be found.
        // error at ProjectDocConfigBuilder#loadJavaSource when qbox parse ScanErrorSource
        HtmlApiDocBuilder.buildApiDoc(config);
    }
}
