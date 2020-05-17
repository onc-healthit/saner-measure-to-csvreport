package org.hl7.fhir.saner.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.hl7.fhir.saner.Mapping;
import org.hl7.fhir.saner.SanerCsvFile;
import org.hl7.fhir.saner.SanerCsvParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@RestController
@RequestMapping("/saner")
public class SanerCSVAdapterController {

	@GetMapping("/json/status")
	public String status() {
		return "Working..";
	}

	@PostMapping("/json/transform")
	public ResponseEntity<StreamingResponseBody> transformJSON(@RequestParam("mappingfile") MultipartFile mappingFile,
			@RequestParam("jsonfile") MultipartFile jsonFile, @RequestParam("source") String source,
			RedirectAttributes redirectAttributes, final HttpServletResponse response) {
		StreamingResponseBody responseBody = null;
		try {
			// Load mapping file
			List<Mapping> mappings = loadMeasureReportMapping(mappingFile.getInputStream(), source);
			SanerCsvFile csvFile = getSanerCSVFile(mappings);

			// Load JSON file
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonFile.getInputStream());
			JsonNode resourceType = jsonNode.get("resourceType");
			if(resourceType != null && resourceType.asText().equals("Bundle"))
			{
				JsonNode entriesNode = jsonNode.get("entry");
				Iterator<JsonNode> entryNodeIter = entriesNode.iterator();
				while(entryNodeIter.hasNext())
				{
					JsonNode entryNode = entryNodeIter.next();
					JsonNode resourceNode = entryNode.get("resource");
					csvFile.addRecord(getMeasureRecord(csvFile, resourceNode));
				}
			}
			else
			{
				// For each header element get values from the JSON
				csvFile.addRecord(getMeasureRecord(csvFile, jsonNode));
			}
			responseBody = new StreamingResponseBody() {
				@Override
				public void writeTo(OutputStream out) throws IOException {
					out.write(csvFile.toString().getBytes());
					out.flush();
				}
			};
		} catch (NullPointerException e) {
			throw new SanerCsvParserException(e);
		} catch (IOException e) {
			throw new SanerCsvParserException(e);
		} catch (Exception e) {
			throw new SanerCsvParserException(e);
		}
		return new ResponseEntity(responseBody, HttpStatus.OK);
	}

	public String getMeasureRecord(SanerCsvFile csvFile, JsonNode jsonNode) {
		List<String> csvHeaders = csvFile.getHeaders();

		StringBuffer record = new StringBuffer();
		for (String header : csvHeaders) {
			if (header.equals("date")) {
				String dateVal = jsonNode.get("date") != null ? jsonNode.get("date").asText() : "";
				record.append(dateVal + ",");
				continue;
			} else if (header.equals("state")) {
				String stateVal = getStateValue(jsonNode);
				record.append(stateVal + ",");
				continue;
			}
			String value = getJSONElementValue(jsonNode, header);
			if (value == null)
				record.append(",");
			else
				record.append(value + ",");
		}
		return record.substring(0, record.lastIndexOf(","));
	}

	public String getStateValue(JsonNode jsonNode) {
		JsonPointer jsonPointer = JsonPointer.compile("/subject/identifier");
		JsonNode node = jsonNode.at(jsonPointer);
		return node.get("value") != null ? node.get("value").asText() : "";
	}

	public SanerCsvFile getSanerCSVFile(List<Mapping> mappings) {
		List<String> header = new ArrayList<String>();
		for (Mapping mapping : mappings) {
			if (mapping.column != null && !mapping.column.isEmpty() && !mapping.getItem().equals("NULL"))
				header.add(mapping.column);
		}
		SanerCsvFile csvFile = new SanerCsvFile(header);
		return csvFile;
	}

	public String getJSONElementValue(JsonNode jsonNode, String mappingElement) {
		JsonPointer jsonPointer = JsonPointer.compile("/group");
		JsonNode groupNodes = jsonNode.at(jsonPointer);
		Iterator<JsonNode> groupNodesIter = groupNodes.iterator();
		while (groupNodesIter.hasNext()) {
			boolean found = false;
			JsonNode groupNode = groupNodesIter.next();
			JsonPointer codeJsonPointer = JsonPointer.compile("/population");
			JsonNode populationNodes = groupNode.at(codeJsonPointer);
			Iterator<JsonNode> populationIter = populationNodes.iterator();
			while (populationIter.hasNext()) {
				JsonNode populationNode = populationIter.next();
				JsonPointer codingJsonPointer = JsonPointer.compile("/code/coding");
				JsonNode codingNode = populationNode.at(codingJsonPointer);
				Iterator<JsonNode> codingIter = codingNode.iterator();
				while (codingIter.hasNext()) {
					JsonNode codeNode = codingIter.next();
					String value = codeNode.get("code").asText();
					if (value != null && value.equals(mappingElement)) {
						found = true;
						break;
					}
				}
				if (found)
					return populationNode.get("count") != null ? populationNode.get("count").asText() : "";

			}
		}
		return "";
	}

	public List<Mapping> loadMeasureReportMapping(InputStream mappingCSVFile, String source) throws IOException {
		CsvSchema schema = CsvSchema.builder().addColumn("column").addColumn("item").addColumn("source")
				.addColumn("group").addColumn("pop").addColumn("data").addColumn("comment").addColumn("shortdescr")
				.addColumn("description").addColumn("instructions").build();
		CsvMapper mapper = new CsvMapper();
		ObjectReader oReader = mapper.readerFor(Mapping.class).with(schema);
		List<Mapping> mappings = new ArrayList<Mapping>();
		try (Reader reader = new InputStreamReader(mappingCSVFile)) {
			MappingIterator<Mapping> mi = oReader.readValues(reader);
			while (mi.hasNext()) {
				Mapping current = mi.next();
				if (current.getSource() == null || current.getSource().isEmpty() || current.getSource().equals("All")
						|| current.getSource().equals(source))
					mappings.add(current);
			}
		}
		return mappings;
	}
}
