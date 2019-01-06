package nxt.http;

import nxt.NxtException;
import nxt.settings.SettingsDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_KEY;
import static nxt.http.JSONResponses.MISSING_VALUE;

class UpdateSettings extends APIServlet.APIRequestHandler {

    static final UpdateSettings instance = new UpdateSettings();

    private UpdateSettings() {
        super(new APITag[] {APITag.SETTINGS}, "key", "value");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String key = request.getParameter("key");
        String value = request.getParameter("value");

        if (key == null) {
            return MISSING_KEY;
        } else if (value == null) {
            return MISSING_VALUE;
        }

        try {
            return JSONData.updateSettings(SettingsDb.updateSettings(key, value));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}