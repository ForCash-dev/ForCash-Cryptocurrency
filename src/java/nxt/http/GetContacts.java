package nxt.http;

import nxt.NxtException;
import nxt.contact.ContactDb;

import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;

class GetContacts extends APIServlet.APIRequestHandler {

    static final GetContacts instance = new GetContacts();

    private GetContacts() {
        super(new APITag[] {APITag.CONTACTS});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest request) throws NxtException {
        JSONObject object = new JSONObject();
        JSONArray contacts = new JSONArray();

        try {
            for (List<String> contact: ContactDb.getContacts()) {
                contacts.add(JSONData.getContact(contact));
            }
        } catch (SQLException e) {
            contacts.clear();
            throw new RuntimeException(e.toString(), e);
        }


        object.put("contacts", contacts);
        return object;
    }
}
