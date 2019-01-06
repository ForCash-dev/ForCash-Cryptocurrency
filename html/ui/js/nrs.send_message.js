var NRS = (function(NRS, $, undefined) {
    NRS.pages.send_message = function(contact) {
        $('.modal').modal('hide');

        var $inputField = $('#send_message_page').find("input[name=recipient], input[name=account_id]").not("[type=hidden]");
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
            $('#send_message_form').find('input[name=recipient], input[name=secretPhrase], input[name=referencedTrasactionFullHash], textarea[name=message]').val('');
            $('#send_message_form').find('.callout').hide();
        }
    };

    NRS.incoming.send_message = function() {
        NRS.loadPage("send_message");
    };

    return NRS;
}(NRS || {}, jQuery));
