package org.wipo.wipolex.sitemap.lambda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Export;
import com.amazonaws.services.cloudformation.model.ListExportsRequest;
import com.amazonaws.services.cloudformation.model.ListExportsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import cloud.godof.lambda.output.region.Handler;
import cloud.godof.lambda.output.region.utils.Constants;

@RunWith(MockitoJUnitRunner.class)
public class HandlerTest {
	
	private final String PARENT_PROPERTY = "LambdaFunctionAssociations";
	private final String PROPERTY_NAME = "LambdaFunctionARN";	
	private final String EVENT_TYPE = "EventType";
	private final String ACCOUNT_ID = "000000000000";
	private final String TRANSFORM_ID = ACCOUNT_ID + "::GetOutputRegion";
	private final String LAMBDA_BOT_ARN = "arn:aws:lambda:us-east-1:" + ACCOUNT_ID +":function:dynamic-origin-lambda-bot-detector:1";
	private final String LAMBDA_CHANGE_ARN = "arn:aws:lambda:us-east-1:000000000000:function:dynamic-origin-lambda-change-origin:1";
	private final String REGION_ORIGIN = "eu-central-1";
	private final String REGION_TARGET = "us-east-1";
	
	private final String OUTPUT_NAME_VALUE1 = "lambda-bot-detector-function-arn";
	private final String OUTPUT_NAME_VALUE2 = "lambda-change-origin-function-arn";
	
		
	@Mock
	private Context context;
	@Mock
	private AmazonCloudFormation client;
	
	@InjectMocks
	private Handler testHandler;
	
	@Before
	public void beforeEach() {
		Mockito.when(context.getLogger()).thenReturn(new LambdaLogger() {
			@Override
			public void log(String string) {
				// TODO Auto-generated method stub
			}
		});
	}

	@Test
	public void getOutputsListOKTest() throws Exception {
		Map<String, Object> event = buildEvent(true);
		
		Mockito.when(client.listExports(Mockito.any(ListExportsRequest.class))).thenReturn(mockFullListExportsResponse());
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildSuccessfulResult(event, true);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
		
		ObjectMapper mapper = new ObjectMapper();
		
		Assert.assertTrue(mapper.writeValueAsString(expectedResult.get(Constants.FRAGMENT)).equals(mapper.writeValueAsString(response.get(Constants.FRAGMENT))));
	}
	
	@Test
	public void getOutputsListWithMultipleExportsOKTest() throws Exception {
		Map<String, Object> event = buildEvent(true);
		
		Mockito.when(client.listExports(Mockito.any(ListExportsRequest.class))).thenReturn(mockListExportsResponse1(), mockListExportsResponse2());
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildSuccessfulResult(event, true);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
		
		ObjectMapper mapper = new ObjectMapper();
		
		Assert.assertTrue(mapper.writeValueAsString(expectedResult.get(Constants.FRAGMENT)).equals(mapper.writeValueAsString(response.get(Constants.FRAGMENT))));
	}
	
	@Test
	public void getOutputOKTest() throws Exception {
		Map<String, Object> event = buildEvent(false);
		
		Mockito.when(client.listExports(Mockito.any(ListExportsRequest.class))).thenReturn(mockFullListExportsResponse());
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildSuccessfulResult(event, false);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
		
		ObjectMapper mapper = new ObjectMapper();
		
		Assert.assertTrue(mapper.writeValueAsString(expectedResult.get(Constants.FRAGMENT)).equals(mapper.writeValueAsString(response.get(Constants.FRAGMENT))));
	}
	
	@Test
	public void noParamsTest() {
		Map<String, Object> event = buildEvent(false);
		event.remove(Constants.PARAMS);
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildFailedResult(event);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
	}
	
	@Test
	public void missingParamsTest() {
		Map<String, Object> event = buildEvent(false);
		final Map<String, Object> params = (Map<String, Object>) event.getOrDefault(Constants.PARAMS, new HashMap<>());
		params.remove(Constants.PROPERTY_NAME);
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildFailedResult(event);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
	}
	
	@Test
	public void missingParentPropertyParamTest() {
		Map<String, Object> event = buildEvent(true);
		final Map<String, Object> params = (Map<String, Object>) event.getOrDefault(Constants.PARAMS, new HashMap<>());
		params.remove(Constants.PARENT_PROPERTY);
		
		Map<String, Object> response = testHandler.handleRequest(event, context);
		
		Map<String, Object> expectedResult = buildFailedResult(event);
		
		Assert.assertEquals(expectedResult.get(Constants.REQUESTID), response.get(Constants.REQUESTID));		
		Assert.assertEquals(expectedResult.get(Constants.STATUS), response.get(Constants.STATUS));
	}
	
	private Map<String, Object> buildSuccessfulResult(Map<String, Object> event, boolean isList) {
		Map<String, Object> expectedResult = new HashMap<>();
		expectedResult.put(Constants.REQUESTID, event.get(Constants.REQUESTID));
		
		Map<String, Object> fragment;
		List<String> outputNameValues;
		if (isList) {
			outputNameValues = Arrays.asList(LAMBDA_BOT_ARN, LAMBDA_CHANGE_ARN);
			fragment = buildFragmentList();
			
			List<Map<String, Object>> elements = (List<Map<String, Object>>) fragment.get(PARENT_PROPERTY);
			
			for(int i=0; i < outputNameValues.size(); i++) {
				String outputName = outputNameValues.get(i);
				Map<String, Object> element = elements.get(i);
				element.put(PROPERTY_NAME, outputName);
			}
			
		} else {
			fragment = buildFragmentObject();
			fragment.put(PROPERTY_NAME, LAMBDA_BOT_ARN);
		}
		
		expectedResult.put(Constants.FRAGMENT, fragment);
		expectedResult.put(Constants.STATUS, Constants.SUCCESS);
	
		return expectedResult;
	}
	
	private Map<String, Object> buildFailedResult(Map<String, Object> event) {
		Map<String, Object> expectedResult = new HashMap<>();
		expectedResult.put(Constants.REQUESTID, event.get(Constants.REQUESTID));
		expectedResult.put(Constants.STATUS, Constants.FAILURE);
	
		return expectedResult;
	}
	
	private Map<String, Object> buildEvent(boolean isList) {
		Map<String, Object> event = new HashMap<>();
		Map<String, Object> parameters = new HashMap<>();
		
		event.put(Constants.ACCOUNTID, ACCOUNT_ID);
		if (isList) {			
			event.put(Constants.FRAGMENT, buildFragmentList());
			parameters.put(Constants.OUTPUT_NAMES, Arrays.asList(OUTPUT_NAME_VALUE1, OUTPUT_NAME_VALUE2));
		} else {
			event.put(Constants.FRAGMENT, buildFragmentObject());
			parameters.put(Constants.OUTPUT_NAMES, OUTPUT_NAME_VALUE1);
		}
		event.put(Constants.TRANSFORM_ID, TRANSFORM_ID);
		event.put(Constants.REQUESTID, UUID.randomUUID().toString());
		event.put(Constants.REGION, REGION_ORIGIN);
		
		parameters.put(Constants.PARENT_PROPERTY, PARENT_PROPERTY);
		parameters.put(Constants.PROPERTY_NAME, PROPERTY_NAME);
		parameters.put(Constants.TARGET_REGION, REGION_TARGET);
		
		event.put(Constants.PARAMS, parameters);
		
		return event;
	}
	
	private Map<String, Object> buildFragmentList() {
		Map<String, Object> fragment = new HashMap<>();
		fragment.put("ViewerProtocolPolicy", "redirect-to-https");
		
		List<Object> elements = new ArrayList<>();
		Map<String, Object> element1 = new HashMap<>();
		element1.put(EVENT_TYPE, "viewer-request");
		
		Map<String, Object> element2 = new HashMap<>();
		element2.put(EVENT_TYPE, "origin-request");
		
		elements.add(element1);
		elements.add(element2);
		
		fragment.put(PARENT_PROPERTY, elements);
		
		return fragment;
	}
	
	private Map<String, Object> buildFragmentObject() {
		Map<String, Object> fragment = new HashMap<>();
		fragment.put(EVENT_TYPE, "viewer-request");
		
		return fragment;
	}
	
	private ListExportsResult mockFullListExportsResponse() {
		ListExportsResult result = new ListExportsResult();
		
		List<Export> exports = new ArrayList<Export>();
		
		Export export1 = new Export();
		export1.setExportingStackId("arn:aws:cloudformation:us-east-1:" + ACCOUNT_ID +":stack/dynamic-origin-DynamicOriginLambdaStack-BA3NB7XQCA88/071b1590-a5c4-11ea-b623-0ad23187ddb2");
		export1.setName(OUTPUT_NAME_VALUE1);
		export1.setValue(LAMBDA_BOT_ARN);
		
		Export export2 = new Export();
		export2.setExportingStackId("arn:aws:cloudformation:us-east-1:000000000000:stack/dynamic-origin-DynamicOriginLambdaStack-BA3NB7XQCA88/071b1590-a5c4-11ea-b623-0ad23187ddb2");
		export2.setName(OUTPUT_NAME_VALUE2);
		export2.setValue(LAMBDA_CHANGE_ARN);
		
		exports.add(export1);
		exports.add(export2);
		
		result.setExports(exports);
		
		return result;
	}
	
	private ListExportsResult mockListExportsResponse1() {
		ListExportsResult result = new ListExportsResult();
		result.setNextToken(UUID.randomUUID().toString());
		
		List<Export> exports = new ArrayList<Export>();
		
		Export export = new Export();
		export.setExportingStackId("arn:aws:cloudformation:us-east-1:" + ACCOUNT_ID +":stack/dynamic-origin-DynamicOriginLambdaStack-BA3NB7XQCA88/071b1590-a5c4-11ea-b623-0ad23187ddb2");
		export.setName(OUTPUT_NAME_VALUE1);
		export.setValue(LAMBDA_BOT_ARN);
		
		exports.add(export);
		
		result.setExports(exports);
		
		return result;
	}
	
	private ListExportsResult mockListExportsResponse2() {
		ListExportsResult result = new ListExportsResult();
		
		List<Export> exports = new ArrayList<Export>();
		
		Export export = new Export();
		export.setExportingStackId("arn:aws:cloudformation:us-east-1:000000000000:stack/dynamic-origin-DynamicOriginLambdaStack-BA3NB7XQCA88/071b1590-a5c4-11ea-b623-0ad23187ddb2");
		export.setName(OUTPUT_NAME_VALUE2);
		export.setValue(LAMBDA_CHANGE_ARN);
		
		exports.add(export);
		
		result.setExports(exports);
		
		return result;
	}
	
}