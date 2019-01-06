package nxt.http;

import nxt.NxtException;
import nxt.contact.ContactDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_BOTH;
import static nxt.http.JSONResponses.MISSING_NONE;

class GetContact extends APIServlet.APIRequestHandler {

    static final GetContact instance = new GetContact();

    private GetContact() {
        super(new APITag[] {APITag.CONTACTS}, "address", "nickname");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String address = request.getParameter("address");
        String nickname = request.getParameter("nickname");

        boolean isAddress = false, isName = false;

        if (address != null) {
            isAddress = true;
        }

        if (nickname != null) {
            isName = true;
        }

        if (isAddress && isName) {
            return MISSING_NONE;
        } else if (!isName && !isAddress) {
            return MISSING_BOTH;
        }

        try {
            if (isAddress) {
                return JSONData.getContact(ContactDb.getContactByAddress(address));
            } else {
                return JSONData.getContact(ContactDb.getContactByName(nickname));
            }
        } catch (SQLException e) {
            //return JSONData.getContact(null);
            throw new RuntimeException(e.toString(), e);
        }
    }
}