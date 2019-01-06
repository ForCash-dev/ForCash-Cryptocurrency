package nxt.contact;

import nxt.Db;
import nxt.util.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContactDb {
    public static final int TRANSACTION_OK = 0;

    public static final int ADD_USER_EXISTING_ADDRESS = 1;
    public static final int ADD_USER_EXISTING_NAME = 2;

    public static final int UPDATE_USER_ADDRESS_NOT_EXISTING = 1;
    public static final int UPDATE_USER_EXISTS = 2;
    public static final int UPDATE_NICKNAME_NOT_SET = 4;

    public static final int SQL_ERROR = 3;

    public static final String NICKNAME = "nickname";
    public static final String ADDRESS = "address";
    public static final String EMAIL = "email";
    public static final String DESCRIPTION = "description";

    public static final String SUCCESSFULL = "successfull";
    public static final String STATUS = "status";

    public static int saveContact(String contactNickname, String contactAddress, String contactEmail, String contactDescription) throws SQLException {
        if (getContactByAddress(contactAddress) != null) {
            return ADD_USER_EXISTING_ADDRESS;
        } else if (getContactByName(contactNickname) != null) {
            return ADD_USER_EXISTING_NAME;
        }

        Connection con = Db.db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO contact (address, nickname, email, description) values (?, ?, ?, ?)");
        pstmt.setString(1, contactAddress);
        pstmt.setString(2, contactNickname);
        pstmt.setString(3, contactEmail);
        pstmt.setString(4, contactDescription);
        pstmt.executeUpdate();

        con.close();

        return TRANSACTION_OK;
    }

    public static int updateContact(String contactNickname, String contactAddress, String contactEmail, String contactDescription) throws SQLException {
        if (contactNickname == null) {
            return UPDATE_NICKNAME_NOT_SET;
        } else if (getContactByAddress(contactAddress) == null) {
            return UPDATE_USER_ADDRESS_NOT_EXISTING;
        }

        List<String> contact = getContactByName(contactNickname);
        if (contact != null && !contact.get(0).equals(contactAddress)) {
            return UPDATE_USER_EXISTS;
        }

        Connection con = Db.db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("UPDATE contact SET nickname = ?,  email = ?,  description = ? WHERE address = ?");
        pstmt.setString(1, contactNickname);
        pstmt.setString(2, contactEmail);
        pstmt.setString(3, contactDescription);
        pstmt.setString(4, contactAddress);
        pstmt.executeUpdate();

        if (!con.isClosed()) {
            con.close();
        }

        return TRANSACTION_OK;
    }

    public static boolean deleteContact(String contactAddress) throws SQLException {
        Connection con = Db.db.getConnection();

        PreparedStatement pstmt = con.prepareStatement("DELETE FROM contact WHERE address = ?");
        pstmt.setString(1, contactAddress);
        pstmt.executeUpdate();

        if (!con.isClosed()) {
            con.close();
        }

        return getContactByAddress(contactAddress) == null;
    }

    public static List<String> getContactByAddress(String address) throws SQLException {
        return getContact(true, address);
    }

    public static List<String> getContactByName(String name) throws SQLException {
        return getContact(false, name);
    }

    public static List<String> getContact(boolean byAddress, String contactInfo) throws SQLException {
        Connection con = Db.db.getConnection();

        PreparedStatement pstmt;
        if (byAddress) {
            pstmt = con.prepareStatement("SELECT * FROM contact WHERE address = ?");
        } else {
            pstmt = con.prepareStatement("SELECT * FROM contact WHERE nickname = ?");
        }

        pstmt.setString(1, contactInfo);

        List<String> contact = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                contact.add(rs.getString(ADDRESS));
                contact.add(rs.getString(NICKNAME));
                contact.add(rs.getString(EMAIL));
                contact.add(rs.getString(DESCRIPTION));

                if (!con.isClosed()) {
                    con.close();
                }

                return contact;
            }
        }

        if (!con.isClosed()) {
            con.close();
        }

        return null;
    }

    public static List<List<String>> getContacts() throws SQLException {
        Connection con = Db.db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM contact");

        List<List<String>> contactsInfo = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                List<String> contactInfo = new ArrayList<>();
                contactInfo.add(rs.getString(ADDRESS));
                contactInfo.add(rs.getString(NICKNAME));
                contactInfo.add(rs.getString(EMAIL));
                contactInfo.add(rs.getString(DESCRIPTION));
                contactsInfo.add(contactInfo);
            }
        }

        if (!con.isClosed()) {
            con.close();
        }

        return contactsInfo;
    }
}
