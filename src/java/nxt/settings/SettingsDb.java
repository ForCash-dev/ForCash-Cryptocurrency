package nxt.settings;

import nxt.Db;
import nxt.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class SettingsDb {
    public static final String KEY = "settingKey";
    public static final String VALUE = "settingValue";

    private static final int TRANSACTION_OK = 0;
    private static final int KEY_NOT_VALID = 1;

    private static final int INCORRECT_LANGUAGE = 2;
    private static final int INCORRECT_HOUR_FORMAT = 3;
    private static final int INCORRECT_REMEMBER_PASSPHRASE = 4;
    private static final int INCORRECT_ITEMS_PER_PAGE = 4;

    private static final int INCORRECT_FEE_WARNING = 5;
    private static final int INCORRECT_AMOUNT_WARNING = 6;

    private static final String[] choices = {"language", "hourFormat", "rememberPassphrase", "itemsPerPage", "feeWarning", "amountWarning"};

    private static final String[] languageChoices = {"de", "en", "es", "fr", "it", "cs"};
    private static final String[] hourFormatChoices = {"12h", "24h"};
    private static final String[] booleanChoices = {"yes", "no"};
    private static final String[] itemsPerPageChoices = {"15", "30", "45", "60", "75"};

    private static boolean isValidValue(String[] set, String value) {
        return Arrays.asList(set).contains(value);
    }

    private static int isValidCombination(String key, String value) {
        if (!isValidKey(key)) {
            return KEY_NOT_VALID;
        }

        switch (key) {
            case "language":
                if (!isValidValue(languageChoices, value)) {
                    return INCORRECT_LANGUAGE;
                }
                break;

            case "hourFormat":
                if (!isValidValue(hourFormatChoices, value)) {
                    return INCORRECT_HOUR_FORMAT;
                }
                break;

            case "rememberPassphrase":
                if (!isValidValue(booleanChoices, value)) {
                    return INCORRECT_REMEMBER_PASSPHRASE;
                }
                break;

            case "itemsPerPage":
                if (!isValidValue(itemsPerPageChoices, value)) {
                    return INCORRECT_ITEMS_PER_PAGE;
                }
                break;

            case "feeWarning":
                if (!(value.length() >= 7 && value.length() <= 14) && !value.matches("\\d")) {
                    return INCORRECT_FEE_WARNING;
                }
                break;

            case "amountWarning":
                if (!(value.length() >= 7 && value.length() <= 14) && !value.matches("\\d")) {
                    return INCORRECT_AMOUNT_WARNING;
                }
                break;
        }

        return TRANSACTION_OK;
    }

    private static boolean isValidKey(String key) {
        return Arrays.asList(choices).contains(key);
    }

    public static int updateSettings(String key, String value) throws SQLException {
        int result;
        if ((result = isValidCombination(key, value)) != TRANSACTION_OK) {
            return result;
        }

        Connection con = Db.db.getConnection();

        PreparedStatement pstmt;

        if (getSetting(key) == null) {
            pstmt = con.prepareStatement("INSERT INTO setting (settingKey, settingValue) values (?, ?)");
            pstmt.setString(1, key);
            pstmt.setString(2, value);
        } else {
            pstmt = con.prepareStatement("UPDATE setting SET settingValue = ? WHERE settingKey = ?");
            pstmt.setString(1, value);
            pstmt.setString(2, key);
        }

        pstmt.executeUpdate();

        if (!con.isClosed()) {
            con.close();
        }

        return TRANSACTION_OK;
    }

    public static String getSetting(String key) throws SQLException {
        if (!isValidKey(key)) {
            return null;
        }

        String result = null;

        Connection con = Db.db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM setting WHERE settingKey = ?");
        pstmt.setString(1, key);

        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                result = rs.getString(VALUE);
            }
        }

        if (!con.isClosed()) {
            con.close();
        }

        return result;
    }

    public static Map<String, String> getSettings() throws SQLException {
        Connection con = Db.db.getConnection();
        PreparedStatement pstmt = con.prepareStatement("SELECT * FROM setting");

        Map<String, String> settingsInfo = new TreeMap<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                settingsInfo.put(rs.getString(KEY), rs.getString(VALUE));
            }
        }

        if (!con.isClosed()) {
            con.close();
        }

        return settingsInfo;
    }
}
