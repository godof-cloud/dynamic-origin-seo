package cloud.godof.lambda.output.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.Export;
import com.amazonaws.services.cloudformation.model.ListExportsRequest;
import com.amazonaws.services.cloudformation.model.ListExportsResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.util.CollectionUtils;
import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cloud.godof.lambda.output.region.utils.Constants;

public class Handler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

	private LambdaLogger logger;
	private AmazonCloudFormation client;
	
	@Override
	public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
		Map<String, Object> responseMap = new HashMap<String, Object>();
		
		try {
			logger = context.getLogger();
			
			ObjectMapper mapper = new ObjectMapper();
			logger.log("Event: " + mapper.writeValueAsString(event));
			
			responseMap.put(Constants.REQUESTID, event.get(Constants.REQUESTID));
			responseMap.put(Constants.STATUS, Constants.FAILURE);
			
			final Map<String, Object> params = (Map<String, Object>) event.getOrDefault(Constants.PARAMS, new HashMap<>());
			if (params.isEmpty()) {
				logger.log("ERROR :: Params are required");
				return responseMap;
			} else if(!params.containsKey(Constants.TARGET_REGION) || !params.containsKey(Constants.OUTPUT_NAMES) || !params.containsKey(Constants.PROPERTY_NAME)) {
				logger.log("ERROR :: There are params missing");
				return responseMap;
			}
			
			String targetRegion = (String) params.get(Constants.TARGET_REGION);
			String parentProperty = (String) params.get(Constants.PARENT_PROPERTY);
			String propertyName = (String) params.get(Constants.PROPERTY_NAME);
			
			Object outputNamesParam = params.get(Constants.OUTPUT_NAMES);
			
			boolean hasMultipleOutputs = outputNamesParam instanceof List;
			
			List<String> outputNames;
			if (hasMultipleOutputs) {
				if(!params.containsKey(Constants.PARENT_PROPERTY)) {
					logger.log("ERROR :: ParentProperty is missing");
					return responseMap;
				}
				
				logger.log("Received list of output names");
				outputNames = (List<String>) outputNamesParam;
			} else {
				logger.log("Received single output name");
				String outputName = (String) outputNamesParam;
				outputNames = Arrays.asList(outputName);
			}
			
			AmazonCloudFormation client = initCloudformationClient(targetRegion);
			
			List<Export> exports = new ArrayList<>();
			String nextToken = null;
			do {
				ListExportsRequest request = new ListExportsRequest();
				if (!StringUtils.isNullOrEmpty(nextToken)) {
					request.setNextToken(nextToken);
				}
				
				ListExportsResult result = client.listExports(request);	
				exports.addAll(result.getExports());
				
				nextToken = result.getNextToken();
			} while(!StringUtils.isNullOrEmpty(nextToken));
			
			final Map<String, Object> fragment = (Map<String, Object>) event.getOrDefault(Constants.FRAGMENT, new HashMap<String, Object>());
			
			List<String> outputValues = exports.stream().filter(export -> outputNames.contains(export.getName()))
					.map(Export::getValue)
					.collect(Collectors.toList());
			
			if (!CollectionUtils.isNullOrEmpty(outputValues)) {
				if (outputNames.size() > 1) {
					logger.log("Get fragment list");
					fragment.putAll(iterateFragment((Map<String, Object>) fragment, parentProperty, propertyName, outputValues));
				} else {
					logger.log("Get fragment object");
					fragment.put(propertyName, outputValues.get(0));
				}
			}
			
			responseMap.put(Constants.FRAGMENT, fragment);
			responseMap.put(Constants.STATUS, Constants.SUCCESS);
			
			
			logger.log("Response: " + mapper.writeValueAsString(responseMap));
		} catch (JsonProcessingException e) {
			logger.log(e.getMessage());
		}
			
		return responseMap;
	}

	private AmazonCloudFormation initCloudformationClient(String targetRegion) {
		if (client == null) {
			this.client = AmazonCloudFormationClientBuilder.standard().withRegion(targetRegion).build();
		}
		
		return client;
	}
	
	private Map<String, Object> iterateFragment(final Map<String, Object> fragment, String parentProperty, String propertyName, List<String> outputValues) throws JsonProcessingException {
		final Map<String, Object> retFragment = new HashMap<String, Object>();
    	
		ObjectMapper mapper = new ObjectMapper();
		logger.log("Fragment: " + mapper.writeValueAsString(fragment));
		
		if (!StringUtils.isNullOrEmpty(parentProperty)) {
			Object element = fragment.get(parentProperty);
			
			retFragment.put(parentProperty, iterateFragment((List<Object>) element, propertyName, outputValues));
		}
		
    	
    	return retFragment;
    }
	
	private List<Object> iterateFragment(final List<Object> fragment, String propertyName, List<String> outputValues) {
		final List<Object> retFragment = new ArrayList<>();
		
		for(int i=0; outputValues.size() > i; i++) {
			String outputValue = outputValues.get(i);
			Object element = fragment.get(i);
			Map<String, Object> elementClone = cloneMap(element);
			elementClone.put(propertyName, outputValue);
			
			logger.log("Added property: " + propertyName + "with value: " + outputValue);
			
    		retFragment.add(elementClone);
		}
    	
    	return retFragment;
    }
	
	private Map<String, Object> cloneMap(Object object) {
		Map<String, Object> clone = new HashMap<String, Object>();
		clone.putAll((Map<String, Object>) object);
		
		return clone;
	}

}
