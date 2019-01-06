/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.userInfoModal = {
		"user": 0
	};

	$("#dashboard_blocks_table, #blocks_table, #polls_table, #contacts_table, #transactions_table, #send_coins_transactions_table, #dashboard_transactions_table, #asset_account, #asset_exchange_ask_orders_table, #transfer_history_table, #asset_exchange_bid_orders_table, #alias_info_table, .dgs_page_contents, .modal-content, #register_alias_modal, #asset_exchange_trade_history_table, #trade_history_table, #ms_open_sell_orders_table, #ms_open_buy_orders_table, #ms_exchanges_history_table, #exchange_history_table").on("click", "a[data-user]", function(e) {
		e.preventDefault();

		var account = $(this).data("user");

		NRS.showAccountModal(account);
	});

	NRS.showAccountModal = function(account) {
		if (NRS.fetchingModalData) {
			return;
		}

		if (typeof account == "object") {
			NRS.userInfoModal.user = account.account;
		} else {
			NRS.userInfoModal.user = account;
			NRS.fetchingModalData = true;
		}

		$("#user_info_modal_account").html(NRS.getAccountFormatted(NRS.userInfoModal.user));

		if (NRS.userInfoModal.user in NRS.contacts) {
			var accountButton = NRS.contacts[NRS.userInfoModal.user].name.escapeHTML();
			$("#user_info_modal_add_as_contact").hide();
		} else {
			var accountButton = NRS.userInfoModal.user;
			$("#user_info_modal_add_as_contact").show();
		}

		$("#user_info_modal_actions button").data("account", accountButton);

		if (NRS.fetchingModalData) {
			NRS.sendRequest("getAccount", {
				"account": NRS.userInfoModal.user
			}, function(response) {
				NRS.processAccountModalData(response);
				NRS.fetchingModalData = false;
			});
		} else {
			NRS.processAccountModalData(account);
		}

		$("#user_info_modal_transactions").show();

		NRS.userInfoModal.transactions();
	}

	NRS.processAccountModalData = function(account) {
		if (account.unconfirmedBalanceNQT == "0") {
			$("#user_info_modal_account_balance").html("0");
		} else {
			$("#user_info_modal_account_balance").html(NRS.formatAmount(account.unconfirmedBalanceNQT) + " FCH");
		}

		if (account.name) {
			$("#user_info_modal_account_name").html(String(account.name).escapeHTML());
			$("#user_info_modal_account_name_container").show();
		} else {
			$("#user_info_modal_account_name_container").hide();
		}

		if (account.description) {
			$("#user_info_description").show();
			$("#user_info_modal_description").html(String(account.description).escapeHTML().nl2br());
		} else {
			$("#user_info_description").hide();
		}

		$("#user_info_modal").modal("show");
	}

	$("#user_info_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".user_info_modal_content").hide();
		$(this).find(".user_info_modal_content table tbody").empty();
		$(this).find(".user_info_modal_content:not(.data-loading,.data-never-loading)").addClass("data-loading");
		$(this).find("ul.nav li.active").removeClass("active");
		$("#user_info_transactions").addClass("active");
		NRS.userInfoModal.user = 0;
	});

	$("#user_info_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".user_info_modal_content").hide();

		var content = $("#user_info_modal_" + tab);

		content.show();

        $(this).parents(".btn-group").find(".text").text($(this).text());

		if (content.hasClass("data-loading")) {
			NRS.userInfoModal[tab]();
		}
	});

	/*some duplicate methods here...*/
	NRS.userInfoModal.transactions = function(type) {
		NRS.sendRequest("getAccountTransactions", {
			"account": NRS.userInfoModal.user,
			"firstIndex": 0,
			"lastIndex": 100
		}, function(response) {
			if (response.transactions && response.transactions.length) {
				var rows = "";

				for (var i = 0; i < response.transactions.length; i++) {
					var transaction = response.transactions[i];

					var transactionType = "Unknown";

					if (transaction.type == 0) {
						transactionType = $.t("ordinary_payment");
					} else if (transaction.type == 1) {
						switch (transaction.subtype) {
							case 0:
								transactionType = $.t("arbitrary_message");
								break;
							case 1:
								transactionType = $.t("alias_assignment");
								break;
							case 2:
								transactionType = $.t("poll_creation");
								break;
							case 3:
								transactionType = $.t("vote_casting");
								break;
							case 4:
								transactionType = $.t("hub_announcement");
								break;
							case 5:
								transactionType = $.t("account_info");
								break;
							case 6:
								if (transaction.attachment.priceNQT == "0") {
									if (transaction.sender == transaction.recipient) {
										transactionType = $.t("alias_sale_cancellation");
									} else {
										transactionType = $.t("alias_transfer");
									}
								} else {
									transactionType = $.t("alias_sale");
								}
								break;
							case 7:
								transactionType = $.t("alias_buy");
								break;
						}
					} else if (transaction.type == 2) {
						switch (transaction.subtype) {
							case 0:
								transactionType = $.t("asset_issuance");
								break;
							case 1:
								transactionType = $.t("asset_transfer");
								break;
							case 2:
								transactionType = $.t("ask_order_placement");
								break;
							case 3:
								transactionType = $.t("bid_order_placement");
								break;
							case 4:
								transactionType = $.t("ask_order_cancellation");
								break;
							case 5:
								transactionType = $.t("bid_order_cancellation");
								break;
						}
					} else if (transaction.type == 3) {
						switch (transaction.subtype) {
							case 0:
								transactionType = $.t("market_listing");
								break;
							case 1:
								transactionType = $.t("market_removal");
								break;
							case 2:
								transactionType = $.t("market_price_change");
								break;
							case 3:
								transactionType = $.t("market_quantity_change");
								break;
							case 4:
								transactionType = $.t("market_purchase");
								break;
							case 5:
								transactionType = $.t("market_delivery");
								break;
							case 6:
								transactionType = $.t("market_feedback");
								break;
							case 7:
								transactionType = $.t("market_refund");
								break;
						}
					} else if (transaction.type == 4) {
						switch (transaction.subtype) {
							case 0:
								transactionType = $.t("balance_leasing");
								break;
						}
					} else if (transaction.type == 5) {
						switch (transaction.subtype) {
							case 0:
								transactionType = $.t("issue_currency");
								break;
							case 1:
								transactionType = $.t("reserve_increase");
								break;
							case 2:
								transactionType = $.t("reserve_claim");
								break;
							case 3:
								transactionType = $.t("currency_transfer");
								break;
							case 4:
								transactionType = $.t("publish_exchange_offer");
								break;
							case 5:
								transactionType = $.t("buy_currency");
								break;
							case 6:
								transactionType = $.t("sell_currency");
								break;
							case 7:
								transactionType = $.t("mint_currency");
								break;
							case 8:
								transactionType = $.t("delete_currency");
								break;
						}
					}

					if (/^FCH\-/i.test(NRS.userInfoModal.user)) {
						var receiving = (transaction.recipientRS == NRS.userInfoModal.user);
					} else {
						var receiving = (transaction.recipient == NRS.userInfoModal.user);
					}

					if (transaction.amountNQT) {
						transaction.amount = new BigInteger(transaction.amountNQT);
						transaction.fee = new BigInteger(transaction.feeNQT);
					}

					var account = (receiving ? "sender" : "recipient");

                    var plusSign = "<i style='position: relative;top: -1px;margin-right: 5px; color: #009514; font-size: 10px;' class='fa fa-plus'></i><span style='color: #009514;'>" + NRS.formatAmount(transaction.amount) + "</span>";
                    var minueSign = "<i style='position: relative;top: -1px;margin-right: 5px; color: red; font-size: 10px;' class='fa fa-minus'></i><span style='color: red;'>" + NRS.formatAmount(transaction.amount) + "</span>";
                    var neutral = "<span>" + NRS.formatAmount(transaction.amount) + "</span>";

                    var typeTransaction = (transaction.type === 0 && receiving ? plusSign : (!receiving && transaction.amount > 0 ? minueSign : neutral));

					rows += "<tr>" +
						"<td><a href='#' data-transaction='" + String(transaction.transaction).escapeHTML() + "'>" + NRS.formatTimestamp(transaction.timestamp) + "</a></td>" +
						"<td>" + transactionType + "</td>" +
						"<td>" + typeTransaction + "</td>" +
						"<td>" + NRS.formatAmount(transaction.fee) + "</td>" +
						"<td>" + NRS.getAccountTitle(transaction, account) + "</td></tr>";
				}

				$("#user_info_modal_transactions_table tbody").empty().append(rows);
				NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
			} else {
				$("#user_info_modal_transactions_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_transactions_table"));
			}
		});
	}

	NRS.userInfoModal.aliases = function() {
		NRS.sendRequest("getAliases", {
			"account": NRS.userInfoModal.user,
			"firstIndex": 0,
			"lastIndex": 100
		}, function(response) {
			var rows = "";

			if (response.aliases && response.aliases.length) {
				var aliases = response.aliases;

				aliases.sort(function(a, b) {
					if (a.aliasName.toLowerCase() > b.aliasName.toLowerCase()) {
						return 1;
					} else if (a.aliasName.toLowerCase() < b.aliasName.toLowerCase()) {
						return -1;
					} else {
						return 0;
					}
				});

				var alias_account_count = 0,
					alias_uri_count = 0,
					empty_alias_count = 0,
					alias_count = aliases.length;

				for (var i = 0; i < alias_count; i++) {
					var alias = aliases[i];

					rows += "<tr data-alias='" + String(alias.aliasName).toLowerCase().escapeHTML() + "'><td class='alias'>" + String(alias.aliasName).escapeHTML() + "</td><td class='uri'>" + (alias.aliasURI.indexOf("http") === 0 ? "<a href='" + String(alias.aliasURI).escapeHTML() + "' target='_blank'>" + String(alias.aliasURI).escapeHTML() + "</a>" : String(alias.aliasURI).escapeHTML()) + "</td></tr>";
					if (!alias.uri) {
						empty_alias_count++;
					} else if (alias.aliasURI.indexOf("http") === 0) {
						alias_uri_count++;
					} else if (alias.aliasURI.indexOf("acct:") === 0 || alias.aliasURI.indexOf("nacc:") === 0) {
						alias_account_count++;
					}
				}
			}

			$("#user_info_modal_aliases_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#user_info_modal_aliases_table"));
		});
	}

	NRS.userInfoModal.marketplace = function() {
		NRS.sendRequest("getDGSGoods", {
			"seller": NRS.userInfoModal.user,
			"firstIndex": 0,
			"lastIndex": 100
		}, function(response) {
			var rows = "";

			if (response.goods && response.goods.length) {
				for (var i = 0; i < response.goods.length; i++) {
					var good = response.goods[i];
					if (good.name.length > 150) {
						good.name = good.name.substring(0, 150) + "...";
					}
					rows += "<tr><td><a href='#' data-goto-goods='" + String(good.goods).escapeHTML() + "' data-seller='" + String(NRS.userInfoModal.user).escapeHTML() + "'>" + String(good.name).escapeHTML() + "</a></td><td>" + NRS.formatAmount(good.priceNQT) + " FCH</td><td>" + NRS.format(good.quantity) + "</td></tr>";
				}
			}

			$("#user_info_modal_marketplace_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#user_info_modal_marketplace_table"));
		});
	}

	NRS.userInfoModal.currencies = function() {
		NRS.sendRequest("getAccountCurrencies+", {
			"account": NRS.userInfoModal.user
		}, function(response) {
			var rows = "";
			if (response.accountCurrencies && response.accountCurrencies.length) {
				for (var i = 0; i < response.accountCurrencies.length; i++) {
					var currency = response.accountCurrencies[i];
					var code = String(currency.code).escapeHTML();
					var decimals = String(currency.decimals).escapeHTML();
					rows += "<tr>" +
						"<td>" +
							"<a href='#' data-transaction='" + String(currency.currency).escapeHTML() + "' >" + code + "</a>" +
						"</td>" +
						"<td>" + currency.name + "</td>" +
						"<td>" + NRS.formatQuantity(currency.unconfirmedUnits, currency.decimals) + "</td>" +
					"</tr>";
				}
			}
			$("#user_info_modal_currencies_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#user_info_modal_currencies_table"));
		});
	}

	NRS.userInfoModal.assets = function() {
		NRS.sendRequest("getAccount", {
			"account": NRS.userInfoModal.user
		}, function(response) {
			if (response.assetBalances && response.assetBalances.length) {
				var assets = {};
				var nrAssets = 0;
				var ignoredAssets = 0;

				for (var i = 0; i < response.assetBalances.length; i++) {
					if (response.assetBalances[i].balanceQNT == "0") {
						ignoredAssets++;

						if (nrAssets + ignoredAssets == response.assetBalances.length) {
							NRS.userInfoModal.addIssuedAssets(assets);
						}
						continue;
					}

					NRS.sendRequest("getAsset", {
						"asset": response.assetBalances[i].asset,
						"_extra": {
							"balanceQNT": response.assetBalances[i].balanceQNT
						}
					}, function(asset, input) {
						asset.asset = input.asset;
						asset.balanceQNT = input["_extra"].balanceQNT;

						assets[asset.asset] = asset;
						nrAssets++;

						if (nrAssets + ignoredAssets == response.assetBalances.length) {
							NRS.userInfoModal.addIssuedAssets(assets);
						}
					});
				}
			} else {
				NRS.userInfoModal.addIssuedAssets({});
			}
		});
	}

	NRS.userInfoModal.trade_history = function() {
		NRS.sendRequest("getTrades", {
			"account": NRS.userInfoModal.user,
			"firstIndex": 0,
			"lastIndex": 100
		}, function(response, input) {
			var rows = "";

			if (response.trades && response.trades.length) {
				var trades = response.trades;

				var rows = "";

				for (var i = 0; i < trades.length; i++) {
					trades[i].priceNQT = new BigInteger(trades[i].priceNQT);
					trades[i].quantityQNT = new BigInteger(trades[i].quantityQNT);
					trades[i].totalNQT = new BigInteger(NRS.calculateOrderTotalNQT(trades[i].priceNQT, trades[i].quantityQNT));

					var type = (trades[i].buyerRS == NRS.userInfoModal.user ? "buy" : "sell");

					rows += "<tr><td><a href='#' data-goto-asset='" + String(trades[i].asset).escapeHTML() + "'>" + String(trades[i].name).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(trades[i].timestamp) + "</td><td style='color:" + (type == "buy" ? "green" : "red") + "'>" + $.t(type) + "</td><td>" + NRS.formatQuantity(trades[i].quantityQNT, trades[i].decimals) + "</td><td class='asset_price'>" + NRS.formatOrderPricePerWholeQNT(trades[i].priceNQT, trades[i].decimals) + "</td><td style='color:" + (type == "buy" ? "red" : "green") + "'>" + NRS.formatAmount(trades[i].totalNQT) + "</td></tr>";
				}
			}

			$("#user_info_modal_trade_history_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#user_info_modal_trade_history_table"));
		});
	}

	NRS.userInfoModal.addIssuedAssets = function(assets) {
		NRS.sendRequest("getAssetsByIssuer", {
			"account": NRS.userInfoModal.user
		}, function(response) {
			if (response.assets && response.assets[0] && response.assets[0].length) {
				$.each(response.assets[0], function(key, issuedAsset) {
					if (assets[issuedAsset.asset]) {
						assets[issuedAsset.asset].issued = true;
					} else {
						issuedAsset.balanceQNT = "0";
						issuedAsset.issued = true;
						assets[issuedAsset.asset] = issuedAsset;
					}
				});

				NRS.userInfoModal.assetsLoaded(assets);
			} else if (!$.isEmptyObject(assets)) {
				NRS.userInfoModal.assetsLoaded(assets);
			} else {
				$("#user_info_modal_assets_table tbody").empty();
				NRS.dataLoadFinished($("#user_info_modal_assets_table"));
			}
		});
	}

	NRS.userInfoModal.assetsLoaded = function(assets) {
		var assetArray = [];
		var rows = "";

		$.each(assets, function(key, asset) {
			assetArray.push(asset);
		});

		assetArray.sort(function(a, b) {
			if (a.issued && b.issued) {
				if (a.name.toLowerCase() > b.name.toLowerCase()) {
					return 1;
				} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
					return -1;
				} else {
					return 0;
				}
			} else if (a.issued) {
				return -1;
			} else if (b.issued) {
				return 1;
			} else {
				if (a.name.toLowerCase() > b.name.toLowerCase()) {
					return 1;
				} else if (a.name.toLowerCase() < b.name.toLowerCase()) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		for (var i = 0; i < assetArray.length; i++) {
			var asset = assetArray[i];

			var percentageAsset = NRS.calculatePercentage(asset.balanceQNT, asset.quantityQNT);

			rows += "<tr" + (asset.issued ? " class='asset_owner'" : "") + "><td><a href='#' data-goto-asset='" + String(asset.asset).escapeHTML() + "'" + (asset.issued ? " style='font-weight:bold'" : "") + ">" + String(asset.name).escapeHTML() + "</a></td><td class='quantity'>" + NRS.formatQuantity(asset.balanceQNT, asset.decimals) + "</td><td>" + NRS.formatQuantity(asset.quantityQNT, asset.decimals) + "</td><td>" + percentageAsset + "%</td></tr>";
		}

		$("#user_info_modal_assets_table tbody").empty().append(rows);

		NRS.dataLoadFinished($("#user_info_modal_assets_table"));
	}

	return NRS;
}(NRS || {}, jQuery));
