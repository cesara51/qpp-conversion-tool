package gov.cms.qpp.conversion.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.jdom2.Element;
import org.junit.Test;

import gov.cms.qpp.conversion.model.Node;
import gov.cms.qpp.conversion.xml.XmlUtils;

public class AciNumeratorDenominatorParserTest {

	@Test
	public void parseAciNumeratorDenominatorAsNode() throws Exception {
		String xmlFragment = XmlUtils.buildString("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">",
				"  <observation classCode=\"OBS\" moodCode=\"EVN\">",
				"    <templateId root=\"2.16.840.1.113883.10.20.27.3.3\"/>",
				"    <code code=\"MSRAGG\" codeSystem=\"2.16.840.1.113883.5.4\" codeSystemName=\"ActCode\" displayName=\"rate aggregation\"/>",
				"    <statusCode code=\"completed\"/>", "    <value xsi:type=\"INT\" value=\"600\"/>",
				"    <methodCode code=\"COUNT\" codeSystem=\"2.16.840.1.113883.5.84\" codeSystemName=\"ObservationMethod\" displayName=\"Count\"/>",
				"  </observation>", "</root>");

		Element dom = XmlUtils.stringToDOM(xmlFragment);

		QppXmlInputParser parser = new QppXmlInputParser();
		parser.setDom(dom);

		Node numDenomNode = parser.parse();

		// the returned Node object from the snippet should be:
		// a top level placeholder node with a single child node that has the
		// "rateAggregationDenominator" key in it

		assertThat("returned node should not be null", numDenomNode, is(not(nullValue())));

		assertThat("returned node should have one child node", numDenomNode.getChildNodes().size(), is(1));

		assertThat("aci numerator/denominator value should be 600",
				(String) numDenomNode.getChildNodes().get(0).get("aciNumeratorDenominator"), is("600"));

	}

}