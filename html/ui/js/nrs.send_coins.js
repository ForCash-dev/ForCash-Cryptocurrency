var NRS = (function (NRS, $, undefined) {
    NRS.pages.send_coins = function (contact) {
        $('.modal').modal('hide');

        NRS.calculateBoxes();

        var $inputField = $('#send_coins_page').find("input[name=recipient], input[name=account_id]").not("[type=hidden]");
        $.each($inputField, function() {
            if ($(this).hasClass("noMask")) {
                $(this).mask("FCH-****-****-****-*****", {
                    "noMask": true
                }).removeClass("noMask");
            } else {
                $(this).mask("FCH-****-****-****-*****");
            }
        });

        if (contact) {
            if (!/FCH\-/i.test(contact)) {
                $inputField.addClass("noMask");
            }

            $inputField.val(contact).trigger("checkRecipient");
        } else {
            $('#send-coins-form').find('input[name=recipient], input[name=amountFCH], input[name=secretPhrase], input[name=referencedTrasactionFullHash], textarea[name=message]').val('');
            $('#send_coins_page').find('.callout').hide();
        }
    };

    NRS.incoming.send_coins = function() {
        NRS.loadPage("send_coins");
    };

    NRS.calculateBoxes = function () {
        NRS.sendRequest("getBlocks+", {
            "firstIndex": 0,
            "lastIndex": 14
        }, function(response) {
            if (response.blocks && response.blocks.length && response.blocks.length >= 2) {
                var firstBlock = response.blocks[response.blocks.length - 1];
                var lastBlock = response.blocks[0];

                if (firstBlock && lastBlock) {
                    var time = (lastBlock.timestamp - firstBlock.timestamp) / response.blocks.length;
                    var diameter = time / 2;

                    $("#send_coins_confirmation_time").html("&#8709;" + Math.round(diameter) + "s").removeClass("loading_dots");
                }
            }
        });
    };

    return NRS;
}(NRS || {}, jQuery));
