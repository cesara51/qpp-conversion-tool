package gov.cms.qpp.conversion.api.services;

import gov.cms.qpp.conversion.Converter;
import gov.cms.qpp.conversion.api.model.Constants;
import gov.cms.qpp.conversion.api.model.ErrorMessage;
import gov.cms.qpp.conversion.correlation.PathCorrelator;
import gov.cms.qpp.conversion.encode.JsonWrapper;
import gov.cms.qpp.conversion.model.error.AllErrors;
import gov.cms.qpp.conversion.model.error.Error;
import gov.cms.qpp.conversion.model.error.QppValidationException;
import gov.cms.qpp.conversion.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Implementation for the QPP Validation Service
 */
@Service
public class ValidationServiceImpl implements ValidationService {
	private static final Logger API_LOG = LoggerFactory.getLogger(Constants.API_LOG);
	static final String CONTENT_TYPE = "application/json";


	@Autowired
	private Environment environment;

	private RestTemplate restTemplate = new RestTemplate();

	/**
	 * Validates that the given QPP is valid.
	 *
	 * @param conversionReport A report on the status of the conversion.
	 */
	@Override
	public void validateQpp(final Converter.ConversionReport conversionReport) {
		String validationUrl = environment.getProperty(Constants.VALIDATION_URL_ENV_VARIABLE);

		if (validationUrl == null || validationUrl.isEmpty()) {
			return;
		}

		API_LOG.info("Calling QPP validation");
		conversionReport.getEncoded().stream().forEach(wrapper -> {
			ResponseEntity<String> validationResponse = callValidationEndpoint(validationUrl, wrapper);
			if (HttpStatus.UNPROCESSABLE_ENTITY.equals(validationResponse.getStatusCode())) {
				AllErrors convertedErrors = convertQppValidationErrorsToQrda(validationResponse.getBody(), wrapper);
				conversionReport.setReportDetails(convertedErrors);
				throw new QppValidationException("Converted QPP failed validation", null, conversionReport);
			}
		});
	}

	/**
	 * Calls the validation API end-point.
	 *
	 * @param url The URL of the validation API end-point.
	 * @param qpp The QPP to validate.
	 * @return The response from the validation API end-point.
	 */
	private ResponseEntity<String> callValidationEndpoint(String url, JsonWrapper qpp) {
		restTemplate.setErrorHandler(new NoHandlingErrorHandler());

		HttpEntity<String> request = new HttpEntity<>(qpp.toString(), getHeaders());
		return restTemplate.postForEntity(url, request, String.class);
	}

	/**
	 * Assemble headers for validation call.
	 *
	 * @return the headers
	 */
	HttpHeaders getHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE);
		headers.add(HttpHeaders.ACCEPT, CONTENT_TYPE);

		String submissionToken = environment.getProperty(Constants.SUBMISSION_API_TOKEN_ENV_VARIABLE);
		if (submissionToken != null && !submissionToken.isEmpty()) {
			headers.add(HttpHeaders.AUTHORIZATION,
					"Bearer " + submissionToken);
		}

		return headers;
	}

	/**
	 * Converts the QPP error returned from the validation API to QRDA3 errors
	 *
	 * @param validationResponse The JSON response containing a QPP error.
	 * @param wrapper The QPP that resulted in the QPP error.
	 * @return The QRDA3 errors.
	 */
	AllErrors convertQppValidationErrorsToQrda(String validationResponse, JsonWrapper wrapper) {
		AllErrors errors = new AllErrors();
		Error error = getError(validationResponse);

		error.getDetails().forEach(detail -> {
			String newPath = PathCorrelator.prepPath(detail.getPath(), wrapper);
			detail.setPath(newPath);
		});

		errors.addError(error);

		return errors;
	}

	/**
	 * Deserializes the JSON QPP error into an {@link Error} object.
	 *
	 * @param response The JSON response containing a QPP error.
	 * @return An Error object.
	 */
	private Error getError(String response) {
		return JsonHelper.readJson(new ByteArrayInputStream(response.getBytes(Charset.defaultCharset())),
				ErrorMessage.class)
				.getError();
	}

	/**
	 * A private static class that tells the {@link RestTemplate} to not throw an exception on HTTP status 3xx and 4xx.
	 */
	private static class NoHandlingErrorHandler extends DefaultResponseErrorHandler {
		/**
		 * Empty so it doesn't throw an exception.
		 *
		 * @param response The ClientHttpResponse.
		 * @throws IOException An IOException.
		 */
		@Override
		public void handleError(final ClientHttpResponse response) throws IOException {
			//do nothing
		}
	}
}
