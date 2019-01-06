package nxt.http;

import nxt.NxtException;
import nxt.settings.SettingsDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_KEY;

class GetSetting extends APIServlet.APIRequestHandler {

    static final GetSetting instance = new GetSetting();

    private GetSetting() {
        super(new APITag[] {APITag.SETTINGS}, "key");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String key = request.getParameter("key");

        Logger.logDebugMessage("SETTINGS " + "GetSetting: " + "controlling if API received request");

        if (key == null) {
            return MISSING_KEY;
        }

        Logger.logDebugMessage("SETTINGS " + "GetSetting: " + "API received request " + key);

        try {
            return JSONData.getSetting(SettingsDb.getSetting(key));
        } catch (SQLException e) {
            Logger.logDebugMessage("SETTINGS " + "GetSetting: " + "SQL error" + e.getMessage());
            throw new RuntimeException(e.toString(), e);
        }
    }
}
