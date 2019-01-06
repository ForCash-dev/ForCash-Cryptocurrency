package nxt.http;

import nxt.NxtException;
import nxt.contact.ContactDb;
import nxt.util.JSON;
import nxt.util.Logger;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;

import static nxt.http.JSONResponses.MISSING_ADDRESS;

class DeleteContact extends APIServlet.APIRequestHandler {

    static final DeleteContact instance = new DeleteContact();

    private DeleteContact() {
        super(new APITag[] {APITag.CONTACTS}, "address");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        String address = request.getParameter("address");

        if (address == null) {
            return MISSING_ADDRESS;
        }

        try {
            return JSONData.deleteContact(ContactDb.deleteContact(address));
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
            //return JSONData.deleteContact(false);
        }
    }
}