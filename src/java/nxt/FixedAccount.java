package nxt;

import nxt.util.Convert;

public class FixedAccount {
    public static Account account;

    static void addForgedBalance(long totalFeeNQT) {
        if (!splitForgingActive()) {
            return;
        }

        long id = Convert.parseAccountId(Constants.FORGE_FIXED_ACCOUNT);

        account = Account.addOrGetAccount(id);

        if (account != null) {
            account.addToBalanceAndUnconfirmedBalanceNQT(totalFeeNQT);
            account.addToForgedBalanceNQT(totalFeeNQT);
        }
    }

    static boolean splitForgingActive() {
        Block lastBlock = Nxt.getBlockchain().getLastBlock();

        return (lastBlock != null && lastBlock.getHeight() >= Constants.GENESIS_FORGING_BLOCK);
    }
}
