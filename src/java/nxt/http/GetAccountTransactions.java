package nxt.http;

import nxt.Account;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountTransactions extends APIServlet.APIRequestHandler {

    static final GetAccountTransactions instance = new GetAccountTransactions();

    private GetAccountTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS}, "account", "timestamp", "type", "direction", "subtype", "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int timestamp = ParameterParser.getTimestamp(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);
        ParameterParser.TransactionDirection direction = ParameterParser.getDirection(req);

        boolean withMessage = "true".equalsIgnoreCase(req.getParameter("withMessage"));

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        try (DbIterator<? extends Transaction> iterator = Nxt.getBlockchain().getTransactions(account, numberOfConfirmations, type, subtype, timestamp,
                withMessage, firstIndex, lastIndex)) {
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();

                switch (direction) {
                    case INCOMING:
                        if (transaction.getRecipientId() == account.getId()) {
                            transactions.add(JSONData.transaction(transaction));
                        }
                        break;

                    case OUTGOING:
                        if (transaction.getRecipientId() != account.getId()) {
                            transactions.add(JSONData.transaction(transaction));
                        }
                        break;

                    case BOTH:
                        transactions.add(JSONData.transaction(transaction));
                        break;
                }
            }
        }

        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }

}
