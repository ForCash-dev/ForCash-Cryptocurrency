package nxt.http;

import nxt.NxtException;
import nxt.contact.ContactDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_ADDRESS;
import static nxt.http.JSONResponses.MISSING_NAME;

class UpdateContact extends APIServlet.APIRequestHandler {

    static final UpdateContact instance = new UpdateContact();

    private UpdateContact() {
        super(new APITag[] {APITag.CONTACTS}, "address", "nickname", "email", "description");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String address = request.getParameter("address");
        String nickname = request.getParameter("nickname");
        String email = request.getParameter("email");
        String description = request.getParameter("description");

        Logger.logDebugMessage("CONTACT " + "UpdateContact: " + "controling if API received address and nickname");

        if (address == null) {
            return MISSING_ADDRESS;
        } else if (nickname == null) {
            return MISSING_NAME;
        }

        Logger.logDebugMessage("CONTACT " + "UpdateContact: " + "API received address and nickname");

        try {
            return JSONData.updateContact(ContactDb.updateContact(nickname, address, email, description));
        } catch (SQLException e) {
            Logger.logDebugMessage("CONTACT " + "UpdateContact: " + "SQL error" + e.getMessage());
            throw new RuntimeException(e.toString(), e);
            //return JSONData.updateContact(ContactDb.SQL_ERROR);
        }
    }

}