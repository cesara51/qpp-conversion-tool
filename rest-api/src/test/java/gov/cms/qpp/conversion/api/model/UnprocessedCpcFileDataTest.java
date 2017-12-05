package gov.cms.qpp.conversion.api.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Date;
import java.util.stream.Stream;

import static com.google.common.truth.Truth.assertThat;

class UnprocessedCpcFileDataTest {

	static Stream<String> submissionLocatorProvider() {
		return Stream.of("000099f2-1f9e-4261-8d60-e4bc294386d7", "006ce223-cbf2-4509-9159-a93524662985");
	}

	static Stream<String> fileNameProvider() {
		return Stream.of("valid.xml", "aci_moof.xml");
	}

	static Stream<String> apmIdProvider() {
		return Stream.of("T02789", "KF5RGI");
	}

	static Stream<Date> createdDateProvider() {
		return Stream.of(new Date(954982730L), new Date(23546L));
	}

	static Stream<Boolean> overallSuccessProvider() {
		return Stream.of(Boolean.TRUE, Boolean.FALSE);
	}

	static Stream<Metadata> metadataProvider() {
		return submissionLocatorProvider()
			.flatMap(submissionLocator -> fileNameProvider()
				.flatMap(fileName -> apmIdProvider()
					.flatMap(apmId -> createdDateProvider()
						.flatMap(createdDate -> overallSuccessProvider()
							.map(overallSuccess -> {
			Metadata metadata = new Metadata();
			metadata.setSubmissionLocator(submissionLocator);
			metadata.setFileName(fileName);
			metadata.setApm(apmId);
			metadata.setCreatedDate(createdDate);
			metadata.setOverallStatus(overallSuccess);
			return metadata;
		})))));
	}

	@ParameterizedTest
	@MethodSource("metadataProvider")
	void testConstructor(Metadata metadata) {

		UnprocessedCpcFileData cpcFileData = new UnprocessedCpcFileData(metadata);

		assertThat(cpcFileData.getFileId()).isEqualTo(metadata.getSubmissionLocator());
		assertThat(cpcFileData.getFilename()).isEqualTo(metadata.getFileName());
		assertThat(cpcFileData.getApm()).isEqualTo(metadata.getApm());
		assertThat(cpcFileData.getConversionDate()).isEqualTo(metadata.getCreatedDate().toString());
		assertThat(cpcFileData.getValidationSuccess()).isEqualTo(metadata.getOverallStatus());
	}
}
