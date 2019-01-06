package nxt.http;

import nxt.NxtException;
import nxt.contact.ContactDb;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_ADDRESS;
import static nxt.http.JSONResponses.MISSING_NAME;

class SaveContact extends APIServlet.APIRequestHandler {

    static final SaveContact instance = new SaveContact();

    private SaveContact() {
        super(new APITag[] {APITag.CONTACTS}, "address", "nickname", "email", "description");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String address = request.getParameter("address");
        String nickname = request.getParameter("nickname");
        String email = request.getParameter("email");
        String description = request.getParameter("description");

        if (address == null) {
            return MISSING_ADDRESS;
        } else if (nickname == null) {
            return MISSING_NAME;
        }

        try {
            return JSONData.saveContact(ContactDb.saveContact(nickname, address, email, description));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
            //return JSONData.saveContact(ContactDb.SQL_ERROR);
        }
    }

}