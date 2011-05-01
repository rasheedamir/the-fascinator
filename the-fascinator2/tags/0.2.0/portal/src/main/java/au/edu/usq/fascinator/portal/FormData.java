package au.edu.usq.fascinator.portal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tapestry5.services.Request;

public class FormData {

    private Map<String, List<String>> parameters;

    public FormData() {
        parameters = new HashMap<String, List<String>>();
    }

    public FormData(Request request) {
        parameters = new HashMap<String, List<String>>();
        for (String name : request.getParameterNames()) {
            List<String> values = Arrays.asList(request.getParameters(name));
            parameters.put(name, values);
        }
    }

    public String get(String name) {
        List<String> values = getValues(name);
        return values == null ? null : values.get(0);
    }

    public List<String> getValues(String name) {
        return parameters.get(name);
    }

    public void clear() {
        parameters.clear();
    }

    @Override
    public String toString() {
        return parameters.toString();
    }
}
