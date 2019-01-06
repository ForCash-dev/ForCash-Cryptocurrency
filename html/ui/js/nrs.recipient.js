/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.automaticallyCheckRecipient = function() {
		var $recipientFields = $("#send_message_recipient, #send_coins_recipient, #send_money_recipient, #transfer_asset_recipient, #transfer_currency_recipient, #send_message_recipient, #add_contact_account_id, #update_contact_account_id, #lease_balance_recipient, #transfer_alias_recipient, #sell_alias_recipient");

		$recipientFields.on("blur", function() {
			$(this).trigger("checkRecipient");
		});

		$recipientFields.on("checkRecipient", function() {
			var value = $(this).val();
			var modal = $(this).closest(".modal");

			if (value && value != "FCH-____-____-____-_____") {
				NRS.checkRecipient(value, modal);
			} else {
				modal.find(".account_info").hide();
			}
		});

		$recipientFields.on("oldRecipientPaste", function() {
			var modal = $(this).closest(".modal");

			var callout = modal.find(".account_info").first();

			callout.removeClass("callout-info callout-danger callout-warning").addClass("callout-danger").html($.t("error_numeric_ids_not_allowed")).show();
		});
	}

	$("#send_message_modal, #send_money_modal, #add_contact_modal").on("show.bs.modal", function(e) {
		var $invoker = $(e.relatedTarget);

		var account = $invoker.data("account");

		if (!account) {
			account = $invoker.data("contact");
		}

		if (account) {
			var $inputField = $(this).find("input[name=recipient], input[name=account_id]").not("[type=hidden]");

			if (!/FCH\-/i.test(account)) {
				$inputField.addClass("noMask");
			}

			$inputField.val(account).trigger("checkRecipient");
		}
	});

    /* Removed variable fee, keeping fees at their minimal
	$("#send_money_amount").on("input", function(e) {
		var amount = parseInt($(this).val(), 10);
		var fee = isNaN(amount) ? 1 : (amount < 500 ? 1 : Math.round(amount / 1000));

		$("#send_money_fee").val(fee);

		$(this).closest(".modal").find(".advanced_fee").html(NRS.formatAmount(NRS.convertToNQT(fee)) + " NXT");
	});
	*/

	//todo later: http://twitter.github.io/typeahead.js/
	$("span.recipient_selector button").on("click", function(e) {
		var $list = $(this).parent().find("ul");
		$list.empty();

		if (NRS.contacts.length > 0) {
            NRS.contacts.forEach(function (contact) {
                $list.append("<li><a href='#' data-contact='" + String(contact.address).escapeHTML() + "'>" + String(contact.nickname).escapeHTML() + "</a></li>");
            });

            $list.each(function () {
                $(this).unbind('click');
            })
		} else {
            $list.append("<li><a style='opacity: 0.5' class='no-contacts-hover'><span>No contacts in your list<span></a></li>");

            $list.each(function () {
            	$(this).find('span').html($.t("no-contacts-list"));
                $(this).on('click', function (e) {
                    e.preventDefault();
                    return false;
                });
            })
		}
	});

	$("span.recipient_selector").on("click", "ul li a", function(e) {
		e.preventDefault();
		$(this).closest("form").find("input[name=converted_account_id]").val("");
		$(this).closest("form").find("input[name=recipient],input[name=account_id]").not("[type=hidden]").trigger("unmask").val($(this).data("contact")).trigger("blur");
	});

	NRS.forms.sendMoneyComplete = function(response, data) {
        var addContactSuffix = "";
        NRS.contacts.forEach(function (contact) {
            if (contact.address === data.recipient) {
                addContactSuffix = "<a href='#' data-account='" + NRS.getAccountFormatted(data, "recipient") + "' data-toggle='modal' data-target='#add_contact_modal' style='text-decoration:underline'>" + $.t("add_recipient_to_contacts_q") + "</a>";
            }
        });

		if (!(data["_extra"] && data["_extra"].convertedAccount) && !(data.recipient in NRS.contacts)) {
			$.growl($.t("success_send_money", {
                "account": data.recipientRS,
                "amount": NRS.convertToFCH(data.amountNQT),
                "fee": NRS.convertToFCH(data.feeNQT)
			}) + addContactSuffix, {
				"type": "success"
			});
		} else {
			$.growl($.t("success_send_money", {
				"account": data.recipientRS,
				"amount": NRS.convertToFCH(data.amountNQT),
				"fee": NRS.convertToFCH(data.feeNQT)
			}), {
				"type": "success"
			});
		}

        $('#send-coins-form').find('input[name=recipient], input[name=amountFCH], input[name=secretPhrase], input[name=referencedTrasactionFullHash], textarea[name=message]').val('');
        $('#send_coins_page').find('.callout').hide();
	}

	NRS.sendMoneyShowAccountInformation = function(accountId) {
		NRS.getAccountError(accountId, function(response) {
			if (response.type == "success") {
				$("#send_money_account_info").hide();
			} else {
				$("#send_money_account_info").html(response.message).show();

			}
		});
	}

	NRS.getAccountError = function(accountId, callback) {
		NRS.sendRequest("getAccount", {
			"account": accountId
		}, function(response) {
			if (response.publicKey) {
				if (response.name){
					callback({
						"type": "info",
						"message": $.t("recipient_info_with_name", {
							"name" : response.name,
							"balance": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response
					});
				}
				else{
					callback({
						"type": "info",
						"message": $.t("recipient_info", {
							"balance": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response
					});
				}
			} else {
				if (response.errorCode) {
					if (response.errorCode == 4) {
						callback({
							"type": "danger",
							"message": $.t("recipient_malformed") + (!/^(FCH\-)/i.test(accountId) ? " " + $.t("recipient_alias_suggestion") : ""),
							"account": null
						});
					} else if (response.errorCode == 5) {
						callback({
							"type": "warning",
							"message": $.t("recipient_unknown_pka"),
							"account": null,
							"noPublicKey": true
						});
					} else {
						callback({
							"type": "danger",
							"message": $.t("recipient_problem") + " " + String(response.errorDescription).escapeHTML(),
							"account": null
						});
					}
				} else {
					callback({
						"type": "warning",
						"message": $.t("recipient_no_public_key_pka", {
							"balance": NRS.formatAmount(response.unconfirmedBalanceNQT, false, true)
						}),
						"account": response,
						"noPublicKey": true
					});
				}
			}
		});
	}

	NRS.correctAddressMistake = function(el) {
		$(el).closest(".modal-body").find("input[name=recipient],input[name=account_id]").val($(el).data("address")).trigger("blur");
	}

	NRS.checkRecipient = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";

		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");
		var merchantInfoField = modal.find("input[name=merchant_info]");
		var recipientPublicKeyField = modal.find("input[name=recipientPublicKey]");

		accountInputField.val("");
		merchantInfoField.val("");

		account = $.trim(account);

		//solomon reed. Btw, this regex can be shortened..
		if (/^(FCH\-)?[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+\-[A-Z0-9]+/i.test(account)) {
			var address = new NxtAddress();

			if (address.set(account)) {
				NRS.getAccountError(account, function(response) {
					if (response.noPublicKey) {
						modal.find(".recipient_public_key").show();
					} else {
						modal.find("input[name=recipientPublicKey]").val("");
						modal.find(".recipient_public_key").hide();
					}
					if (response.account && response.account.description) {
						checkForMerchant(response.account.description, modal);
					}

					var message = response.message.escapeHTML();

					callout.removeClass(classes).addClass("callout-" + response.type).html(message).show();
				});
			} else {
				if (address.guess.length == 1) {
					callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed_suggestion", {
						"recipient": "<span class='malformed_address' data-address='" + String(address.guess[0]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[0], account) + "</span>"
					})).show();
				} else if (address.guess.length > 1) {
					var html = $.t("recipient_malformed_suggestion", {
						"count": address.guess.length
					}) + "<ul>";
					for (var i = 0; i < address.guess.length; i++) {
						html += "<li><span clas='malformed_address' data-address='" + String(address.guess[i]).escapeHTML() + "' onclick='NRS.correctAddressMistake(this);'>" + address.format_guess(address.guess[i], account) + "</span></li>";
					}

					callout.removeClass(classes).addClass("callout-danger").html(html).show();
				} else {
					callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
				}
			}
		} else if (!(/^\d+$/.test(account))) {
			if (NRS.databaseSupport && account.charAt(0) != '@') {
				NRS.database.select("contacts", [{
					"name": account
				}], function(error, contact) {
					if (!error && contact.length) {
						contact = contact[0];
						NRS.getAccountError(contact.accountRS, function(response) {
							if (response.noPublicKey) {
								modal.find(".recipient_public_key").show();
							} else {
								modal.find("input[name=recipientPublicKey]").val("");
								modal.find(".recipient_public_key").hide();
							}
							if (response.account && response.account.description) {
								checkForMerchant(response.account.description, modal);
							}

							callout.removeClass(classes).addClass("callout-" + response.type).html($.t("contact_account_link", {
								"account_id": NRS.getAccountFormatted(contact, "account")
							}) + " " + response.message.escapeHTML()).show();

							if (response.type == "info" || response.type == "warning") {
								accountInputField.val(contact.accountRS);
							}
						});
					} else if (/^[a-z0-9]+$/i.test(account)) {
						NRS.checkRecipientAlias(account, modal);
					} else {
						callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
					}
				});
			} else if (/^[a-z0-9@]+$/i.test(account)) {
				if (account.charAt(0) == '@') {
					account = account.substring(1);
					NRS.checkRecipientAlias(account, modal);
				}
			} else {
				callout.removeClass(classes).addClass("callout-danger").html($.t("recipient_malformed")).show();
			}
		} else {
			callout.removeClass(classes).addClass("callout-danger").html($.t("error_numeric_ids_not_allowed")).show();
		}
	}

	NRS.checkRecipientAlias = function(account, modal) {
		var classes = "callout-info callout-danger callout-warning";
		var callout = modal.find(".account_info").first();
		var accountInputField = modal.find("input[name=converted_account_id]");

		accountInputField.val("");

		NRS.sendRequest("getAlias", {
			"aliasName": account
		}, function(response) {
			if (response.errorCode) {
				callout.removeClass(classes).addClass("callout-danger").html($.t("error_invalid_account_id")).show();
			} else {
				if (response.aliasURI) {
					var alias = String(response.aliasURI);
					var timestamp = response.timestamp;

					var regex_1 = /acct:(.*)@nxt/;
					var regex_2 = /nacc:(.*)/;

					var match = alias.match(regex_1);

					if (!match) {
						match = alias.match(regex_2);
					}

					if (match && match[1]) {
						match[1] = String(match[1]).toUpperCase();

						if (/^\d+$/.test(match[1])) {
							var address = new NxtAddress();

							if (address.set(match[1])) {
								match[1] = address.toString();
							} else {
								accountInputField.val("");
								callout.removeClass(classes).addClass("callout-danger").html($.t("error_invalid_account_id")).show();
								return;
							}
						}

						NRS.getAccountError(match[1], function(response) {
							if (response.noPublicKey) {
								modal.find(".recipient_public_key").show();
							} else {
								modal.find("input[name=recipientPublicKey]").val("");
								modal.find(".recipient_public_key").hide();
							}
							if (response.account && response.account.description) {
								checkForMerchant(response.account.description, modal);
							}

							callout.removeClass(classes).addClass("callout-" + response.type).html($.t("alias_account_link", {
								"account_id": String(match[1]).escapeHTML()
							}) + " " + response.message.escapeHTML() + " " + $.t("alias_last_adjusted", {
								"timestamp": NRS.formatTimestamp(timestamp)
							})).show();

							if (response.type == "info" || response.type == "warning") {
								accountInputField.val(String(match[1]).escapeHTML());
							}
						});
					} else {
						callout.removeClass(classes).addClass("callout-danger").html($.t("alias_account_no_link") + (!alias ? $.t("error_uri_empty") : $.t("uri_is", {
							"uri": String(alias).escapeHTML()
						}))).show();
					}
				} else if (response.aliasName) {
					callout.removeClass(classes).addClass("callout-danger").html($.t("error_alias_empty_uri")).show();
				} else {
					callout.removeClass(classes).addClass("callout-danger").html(response.errorDescription ? $.t("error") + ": " + String(response.errorDescription).escapeHTML() : $.t("error_alias")).show();
				}
			}
		});
	}

	function checkForMerchant(accountInfo, modal) {
		var requestType = modal.find("input[name=request_type]").val();

		if (requestType == "sendMoney" || requestType == "transferAsset") {
			if (accountInfo.match(/merchant/i)) {
				modal.find("input[name=merchant_info]").val(accountInfo);
				var checkbox = modal.find("input[name=add_message]");
				if (!checkbox.is(":checked")) {
					checkbox.prop("checked", true).trigger("change");
				}
			}
		}
	}

	return NRS;
}(NRS || {}, jQuery));
