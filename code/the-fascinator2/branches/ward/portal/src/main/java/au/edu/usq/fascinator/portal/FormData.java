package au.edu.usq.fascinator.portal;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.apache.tapestry5.services.Request;

public class FormData {

    private Map<String, List<String>> parameters;

    private HttpServletRequest hsr;

    public FormData() {
        parameters = new HashMap<String, List<String>>();
    }

    public FormData(Request request) {
        this(request, null);
    }

    public FormData(Request request, HttpServletRequest hsr) {
        parameters = new HashMap<String, List<String>>();
        for (String name : request.getParameterNames()) {
            List<String> values = Arrays.asList(request.getParameters(name));
            parameters.put(name, values);
        }
        this.hsr = hsr;
    }

    public String get(String name) {
        List<String> values = getValues(name);
        return values == null ? null : values.get(0);
    }

    public List<String> getValues(String name) {
        return parameters.get(name);
    }

    public InputStream getInputStream(){
        InputStream iStream = null;
        try {
            iStream = hsr.getInputStream();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return iStream;
    }

    public void clear() {
        parameters.clear();
    }

    @Override
    public String toString() {
        return parameters.toString();
    }
}
