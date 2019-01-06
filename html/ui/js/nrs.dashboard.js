var NRS = (function (NRS, $, undefined) {
    NRS.pages.dashboard = function () {
        //Load transactions
        NRS.getInitialTransactions();

        //Load blocks
        NRS.refreshBlocks();
    };

    NRS.incoming.dashboard = function() {
        NRS.loadPage("dashboard");
    };

    NRS.refreshBlocks = function () {
        var rows = "";

        for (var i = 0; i < NRS.blocks.length; i++) {
            if (i < 10) {
                const block = NRS.blocks[i];

                const height = String(block.height).escapeHTML();

                const generatorTitle = NRS.getAccountTitle(block, "generator");

                const numberOfTransactions = NRS.formatAmount(block.numberOfTransactions);

                const amount = NRS.formatAmount(block.totalAmountNQT);
                const fee =  NRS.formatAmount(block.totalFeeNQT);

                const timestamp = String(block.timestamp).escapeHTML();
                const date = NRS.formatTimestamp(block.timestamp);

                rows += "<tr>" +
                    "<td><a href='#' data-block='" + height + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + height + "</a></td>" +
                    "<td><a href='#' class='user_info' data-user='" + generatorTitle + "'>" + generatorTitle + "</a></td>" +
                    "<td style='text-align: center'>" + numberOfTransactions + "</td>" +
                    "<td style='text-align: center'>" + amount + "</td>" +
                    "<td style='text-align: center;padding-right: 20px;'>" + fee + "</td>" +
                    "<td data-timestamp='" + timestamp + "'>" + date + "</td>" +
                    "</tr>";
            }
        }

        $("#dashboard_blocks_table tbody").empty().append(rows);
    }

    return NRS;
}(NRS || {}, jQuery));
