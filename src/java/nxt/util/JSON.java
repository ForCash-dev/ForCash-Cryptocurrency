package nxt.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public final class JSON {

    private JSON() {} //never

    public final static JSONStreamAware emptyJSON = prepare(new JSONObject());

    public static JSONStreamAware prepare(final JSONObject json) {
        return new JSONStreamAware() {
            private final char[] jsonChars = json.toJSONString().toCharArray();
            @Override
            public void writeJSONString(Writer out) throws IOException {
                out.write(jsonChars);
            }
        };
    }

    public static JSONStreamAware prepareRequest(final JSONObject json) {
        json.put("protocol", 1);
        return prepare(json);
    }

    public static String toString(JSONStreamAware jsonStreamAware) {
        StringWriter stringWriter = new StringWriter();
        try {
            jsonStreamAware.writeJSONString(stringWriter);
        } catch (IOException ignore) {}
        return stringWriter.toString();
    }
}
