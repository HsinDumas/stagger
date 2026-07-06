package com.github.hsindumas.stagger.util;

import com.github.hsindumas.stagger.constants.ApiReqParamInTypeEnum;
import com.github.hsindumas.stagger.model.ApiReqParam;
import com.github.hsindumas.stagger.model.request.CurlRequest;
import com.github.hsindumas.stagger.utils.CurlUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author HsinDumas
 */
public class CurlUtilTest {

	/**
	 * test header name
	 */
	@Test
	public void testHeaderName() {
		ApiReqParam apiReqParam = ApiReqParam.builder()
			.setName("Authorization")
			.setValue("lbEfFvLigPuN2pDMxWaTviVuGwhg74T11geUiNcaYwZ4ZAZB780vkQo8OBMVpZmT")
			.setParamIn(ApiReqParamInTypeEnum.HEADER.getValue());
		CurlRequest builder = CurlRequest.builder();
		builder.setUrl("http://127.0.0.1:8080/region/list")
			.setType("POST")
			.setContentType("application/json")
			.setReqHeaders(Arrays.asList(apiReqParam));
		String curl = CurlUtil.toCurl(builder);
		assertTrue(curl.contains("curl -X POST"));
		assertTrue(curl.contains("http://127.0.0.1:8080/region/list"));
		assertTrue(curl.contains("-H \"Authorization:"));
	}

}
