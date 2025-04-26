package udf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.functions.ScalarFunction;

public class XmlToJsonFunction extends ScalarFunction {

    public String eval(String xml) {
        // Implement XML parsing logic here
        // Convert XML to JSON or other desired format
        ObjectMapper xmlMapper = new ObjectMapper();
        try {
            // Assuming the XML is well-formed and valid
            // Convert XML to JSON
            JsonNode node = xmlMapper.readTree(xml);
            return node.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting XML to JSON", e);
        }
    }
}
