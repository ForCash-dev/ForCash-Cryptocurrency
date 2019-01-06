package nxt.http;

import nxt.NxtException;
import nxt.settings.SettingsDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

class GetSettings extends APIServlet.APIRequestHandler {

    static final GetSettings instance = new GetSettings();

    private GetSettings() {
        super(new APITag[] {APITag.SETTINGS});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {

        try {
            return JSONData.getSettings(SettingsDb.getSettings());
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
}