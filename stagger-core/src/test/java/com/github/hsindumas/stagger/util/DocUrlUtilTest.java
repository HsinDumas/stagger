package com.github.hsindumas.stagger.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.hsindumas.stagger.utils.DocUrlUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author HsinDumas
 */
public class DocUrlUtilTest {

    @Test
    public void getMvcUrls() {
        String baseUrl = "/[/testMultiPathOne/{path}/test, /testMultiPathTwo/{path}/test]";
        List<String> urls = new ArrayList<>();
        urls.add("[/{path2}/abc2");
        urls.add(" /{path2}/abc3]");
        String baseServer = "http://{{host}}:{{port}}";

        String mvcUrls = DocUrlUtil.getMvcUrls(baseServer, baseUrl, urls);
        assertTrue(mvcUrls.contains("http://{{host}}:{{port}}"));
        assertTrue(mvcUrls.contains("testMultiPathOne"));
        assertTrue(mvcUrls.contains("testMultiPathTwo"));
        assertTrue(mvcUrls.contains("abc2"));
        assertTrue(mvcUrls.contains("abc3"));
    }
}
