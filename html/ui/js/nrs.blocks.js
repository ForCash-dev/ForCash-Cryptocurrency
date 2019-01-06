/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.blocksPageType = null;
	NRS.tempBlocks = [];
	var trackBlockchain = false;

	NRS.getBlock = function(blockID, callback, pageRequest) {
		NRS.sendRequest("getBlock" + (pageRequest ? "+" : ""), {
			"block": blockID
		}, function(response) {
			if (response.errorCode && response.errorCode == -1) {
				setTimeout(function (){ NRS.getBlock(blockID, callback, pageRequest); }, 2500);
			} else {
				if (callback) {
					response.block = blockID;
					callback(response);
				}
			}
		}, true);
	}

	NRS.handleInitialBlocks = function(response) {
		if (response.errorCode) {
			NRS.dataLoadFinished($("#dashboard_blocks_table"));
			return;
		}

		NRS.blocks.push(response);

		if (NRS.blocks.length < 10 && response.previousBlock) {
			NRS.getBlock(response.previousBlock, NRS.handleInitialBlocks);
		} else {
			NRS.checkBlockHeight(NRS.blocks[0].height);

			if (NRS.state) {
				//if no new blocks in 6 hours, show blockchain download progress..
				var timeDiff = NRS.state.time - NRS.blocks[0].timestamp;
				if (timeDiff > 60 * 60 * 18) {
					if (timeDiff > 60 * 60 * 24 * 14) {
						NRS.setStateInterval(30);
					} else if (timeDiff > 60 * 60 * 24 * 7) {
						//second to last week
						NRS.setStateInterval(15);
					} else {
						//last week
						NRS.setStateInterval(10);
					}
					NRS.downloadingBlockchain = true;
					if (NRS.inApp) {
						parent.postMessage("downloadingBlockchain", "*");
					}
					$("#nrs_update_explanation > span").hide();
					$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").show();

					$("#show_console").hide();
					NRS.updateBlockchainDownloadProgress();
				} else {
					//continue with faster state intervals if we still haven't reached current block from within 1 hour
					if (timeDiff < 60 * 60) {
						NRS.setStateInterval(30);
						trackBlockchain = false;
					} else {
						NRS.setStateInterval(10);
						trackBlockchain = true;
					}
				}
			}

			var rows = "";

			for (var i = 0; i < NRS.blocks.length; i++) {
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

			var block = NRS.blocks[0];
			$("#nrs_current_block_time").empty().append(NRS.formatTimestamp(block.timestamp));
			$(".nrs_current_block").empty().append(String(block.height).escapeHTML());

			$("#dashboard_blocks_table tbody").empty().append(rows);
			NRS.dataLoadFinished($("#dashboard_blocks_table"));
		}
	}

	NRS.handleNewBlocks = function(response) {
		if (NRS.downloadingBlockchain) {
			//new round started...
			if (NRS.tempBlocks.length == 0 && NRS.state.lastBlock != response.block) {
				return;
			}
		}

		//we have all blocks
		if (response.height - 1 == NRS.lastBlockHeight || NRS.tempBlocks.length == 99) {
			var newBlocks = [];

			//there was only 1 new block (response)
			if (NRS.tempBlocks.length == 0) {
				//remove oldest block, add newest block
				NRS.blocks.unshift(response);
				newBlocks.push(response);
			} else {
				NRS.tempBlocks.push(response);
				//remove oldest blocks, add newest blocks
				[].unshift.apply(NRS.blocks, NRS.tempBlocks);
				newBlocks = NRS.tempBlocks;
				NRS.tempBlocks = [];
			}

			if (NRS.blocks.length > 100) {
				NRS.blocks = NRS.blocks.slice(0, 100);
			}

			NRS.checkBlockHeight(NRS.blocks[0].height);

			NRS.incoming.updateDashboardBlocks(newBlocks);
		} else {
			NRS.tempBlocks.push(response);
			NRS.getBlock(response.previousBlock, NRS.handleNewBlocks);
		}
	}

	NRS.checkBlockHeight = function(blockHeight) {
		if (blockHeight) {
			NRS.lastBlockHeight = blockHeight;
		}

		//no checks needed at the moment
	}

	//we always update the dashboard page..
	NRS.incoming.updateDashboardBlocks = function(newBlocks) {
		var newBlockCount = newBlocks.length;

		if (newBlockCount > 10) {
			newBlocks = newBlocks.slice(0, 10);
			newBlockCount = newBlocks.length;
		}

		if (NRS.downloadingBlockchain) {
			if (NRS.state) {
				var timeDiff = NRS.state.time - NRS.blocks[0].timestamp;
				if (timeDiff < 60 * 60 * 18) {
					if (timeDiff < 60 * 60) {
						NRS.setStateInterval(30);
					} else {
						NRS.setStateInterval(10);
						trackBlockchain = true;
					}
					NRS.downloadingBlockchain = false;
					if (NRS.inApp) {
						parent.postMessage("downloadedBlockchain", "*");
					}
					$("#dashboard_message").hide();
					$("#downloading_blockchain, #nrs_update_explanation_blockchain_sync").hide();
					if (NRS.settings["console_log"] && !NRS.inApp) {
						$("#show_console").show();
					}
					//todo: update the dashboard blocks!
					$.growl($.t("success_blockchain_up_to_date"), {
						"type": "success"
					});
					NRS.checkAliasVersions();
					NRS.checkIfOnAFork();
				} else {
					if (timeDiff > 60 * 60 * 24 * 14) {
						NRS.setStateInterval(30);
					} else if (timeDiff > 60 * 60 * 24 * 7) {
						//second to last week
						NRS.setStateInterval(15);
					} else {
						//last week
						NRS.setStateInterval(10);
					}

					NRS.updateBlockchainDownloadProgress();
				}
			}
		} else if (trackBlockchain) {
			var timeDiff = NRS.state.time - NRS.blocks[0].timestamp;

			//continue with faster state intervals if we still haven't reached current block from within 1 hour
			if (timeDiff < 60 * 60) {
				NRS.setStateInterval(30);
				trackBlockchain = false;
			} else {
				NRS.setStateInterval(10);
			}
		}

		var rows = "";

		for (var i = 0; i < newBlockCount; i++) {
			var block = newBlocks[i];

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
                "<td style='text-align: center; padding-right: 20px !important;'>" + fee + "</td>" +
                "<td data-timestamp='" + timestamp + "'>" + date + "</td>" +
                "</tr>";
		}

		if (newBlockCount == 1) {
			$("#dashboard_blocks_table tbody tr:last").remove();
		} else if (newBlockCount == 10) {
			$("#dashboard_blocks_table tbody").empty();
		} else {
			$("#dashboard_blocks_table tbody tr").slice(10 - newBlockCount).remove();
		}

		var block = NRS.blocks[0];
		$("#nrs_current_block_time").empty().append(NRS.formatTimestamp(block.timestamp));
		$(".nrs_current_block").empty().append(String(block.height).escapeHTML());

		$("#dashboard_blocks_table tbody").prepend(rows);

		//update number of confirmations... perhaps we should also update it in tne NRS.transactions array
		$("#dashboard_transactions_table tr.confirmed td.confirmations").each(function() {
			if ($(this).data("incoming")) {
				$(this).removeData("incoming");
				return true;
			}

			var confirmations = parseInt($(this).data("confirmations"), 10);

			var nrConfirmations = confirmations + newBlocks.length;

			if (confirmations <= 10) {
				$(this).data("confirmations", nrConfirmations);
				$(this).attr("data-content", $.t("x_confirmations", {
					"x": NRS.formatAmount(nrConfirmations, false, true)
				}));

				if (nrConfirmations > 10) {
					nrConfirmations = '10+';
				}
				$(this).html(nrConfirmations);
			} else {
				$(this).attr("data-content", $.t("x_confirmations", {
					"x": NRS.formatAmount(nrConfirmations, false, true)
				}));
			}
		});

        $("#send_coins_transactions_table tr.confirmed td.confirmations").each(function() {
            if ($(this).data("incoming")) {
                $(this).removeData("incoming");
                return true;
            }

            var confirmations = parseInt($(this).data("confirmations"), 10);

            var nrConfirmations = confirmations + newBlocks.length;

            if (confirmations <= 10) {
                $(this).data("confirmations", nrConfirmations);
                $(this).attr("data-content", $.t("x_confirmations", {
                    "x": NRS.formatAmount(nrConfirmations, false, true)
                }));

                if (nrConfirmations > 10) {
                    nrConfirmations = '10+';
                }
                $(this).html(nrConfirmations);
            } else {
                $(this).attr("data-content", $.t("x_confirmations", {
                    "x": NRS.formatAmount(nrConfirmations, false, true)
                }));
            }
        });
	}

	NRS.pages.blocks = function() {
		if (NRS.blocksPageType == "forged_blocks") {
			$("#forged_fees_total_box, #forged_blocks_total_box").show();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").hide();

			NRS.sendRequest("getAccountBlocks+", {
				"account": NRS.account,
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.blocks && response.blocks.length) {
					if (response.blocks.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.blocks.pop();
					}
					NRS.blocksPageLoaded(response.blocks);
				} else {
					NRS.blocksPageLoaded([]);
				}
			});
		} else {
			$("#forged_fees_total_box, #forged_blocks_total_box").hide();
			$("#blocks_transactions_per_hour_box, #blocks_generation_time_box").show();

			NRS.sendRequest("getBlocks+", {
				"firstIndex": NRS.pageNumber * NRS.itemsPerPage - NRS.itemsPerPage,
				"lastIndex": NRS.pageNumber * NRS.itemsPerPage
			}, function(response) {
				if (response.blocks && response.blocks.length) {
					if (response.blocks.length > NRS.itemsPerPage) {
						NRS.hasMorePages = true;
						response.blocks.pop();
					}
					NRS.blocksPageLoaded(response.blocks);
				} else {
					NRS.blocksPageLoaded([]);
				}
			});
		}
	}

	NRS.incoming.blocks = function() {
		NRS.loadPage("blocks");
	}

	NRS.blocksPageLoaded = function(blocks) {
		var rows = "";
		var totalAmount = new BigInteger("0");
		var totalFees = new BigInteger("0");
		var totalTransactions = 0;

		for (var i = 0; i < blocks.length; i++) {
			var block = blocks[i];

			totalAmount = totalAmount.add(new BigInteger(block.totalAmountNQT));

			totalFees = totalFees.add(new BigInteger(block.totalFeeNQT));

			totalTransactions += block.numberOfTransactions;

			rows += "<tr><td><a href='#' data-block='" + String(block.height).escapeHTML() + "' data-blockid='" + String(block.block).escapeHTML() + "' class='block'" + (block.numberOfTransactions > 0 ? " style='font-weight:bold'" : "") + ">" + String(block.height).escapeHTML() + "</a></td><td>" + NRS.formatTimestamp(block.timestamp) + "</td><td>" + NRS.formatAmount(block.totalAmountNQT) + "</td><td>" + NRS.formatAmount(block.totalFeeNQT) + "</td><td>" + NRS.formatAmount(block.numberOfTransactions) + "</td><td>" + (block.generator != NRS.genesis ? "<a href='#' data-user='" + NRS.getAccountFormatted(block, "generator") + "' class='user_info'>" + NRS.getAccountTitle(block, "generator") + "</a>" : $.t("genesis")) + "</td><td>" + NRS.formatVolume(block.payloadLength) + "</td><td>" + Math.round(block.baseTarget / 153722867 * 100).pad(4) + " %</td></tr>";
		}

		if (NRS.blocksPageType == "forged_blocks") {
			NRS.sendRequest("getAccountBlockCount+", {
				"account": NRS.account
			}, function(response) {
				if (response.numberOfBlocks && response.numberOfBlocks > 0) {
					$("#forged_blocks_total").html(response.numberOfBlocks).removeClass("loading_dots");
                    var avgFee = new Big(NRS.accountInfo.forgedBalanceNQT).div(response.numberOfBlocks).div(new Big("100000000")).toFixed(2);
                    $("#blocks_average_fee").html(NRS.formatStyledAmount(NRS.convertToNQT(avgFee))).removeClass("loading_dots");
				} else {
					$("#forged_blocks_total").html(0).removeClass("loading_dots");
					$("#blocks_average_fee").html(0).removeClass("loading_dots");
				}
			});
			$("#forged_fees_total").html(NRS.formatStyledAmount(NRS.accountInfo.forgedBalanceNQT)).removeClass("loading_dots");
			$("#blocks_average_amount").removeClass("loading_dots");
			$("#blocks_average_amount").parent().parent().css('visibility', 'hidden');
			$("#blocks_page .ion-stats-bars").parent().css('visibility', 'hidden');
		} else {
			if (blocks.length) {
				var startingTime = blocks[blocks.length - 1].timestamp;
				var endingTime = blocks[0].timestamp;
				var time = endingTime - startingTime;
			} else {
				var startingTime = endingTime = time = 0;
			}

			if (blocks.length) {
				var averageFee = new Big(totalFees.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);
				var averageAmount = new Big(totalAmount.toString()).div(new Big("100000000")).div(new Big(String(blocks.length))).toFixed(2);
			} else {
				var averageFee = 0;
				var averageAmount = 0;
			}

			averageFee = NRS.convertToNQT(averageFee);
			averageAmount = NRS.convertToNQT(averageAmount);

			if (time == 0) {
				$("#blocks_transactions_per_hour").html("0").removeClass("loading_dots");
			} else {
				$("#blocks_transactions_per_hour").html(Math.round(totalTransactions / (time / 60) * 60)).removeClass("loading_dots");
			}
			$("#blocks_average_generation_time").html(Math.round(time / NRS.itemsPerPage) + "s").removeClass("loading_dots");
			$("#blocks_average_fee").html(NRS.formatStyledAmount(averageFee)).removeClass("loading_dots");
			$("#blocks_average_amount").parent().parent().css('visibility', 'visible');
			$("#blocks_page .ion-stats-bars").parent().css('visibility', 'visible');
			$("#blocks_average_amount").html(NRS.formatStyledAmount(averageAmount)).removeClass("loading_dots");
		}

		NRS.dataLoaded(rows);
	}

	$("#goto_forged_blocks").click(function(e) {
		e.preventDefault();

		$("#blocks_page_type").find(".btn:last").button("toggle");
		NRS.blocksPageType = "forged_blocks";
		NRS.goToPage("blocks");
	});

    $("#blocks_page_type li button").click(function(e) {
        e.preventDefault();

        NRS.blocksPageType = $(this).data("type");

        $("#send_coins_confirmation_time").html("<span>.</span><span>.</span><span>.</span></span>").addClass("loading_dots");
        $("#blocks_average_amount, #blocks_average_fee, #blocks_transactions_per_hour, #blocks_average_generation_time, #forged_blocks_total, #forged_fees_total").html("<span>.</span><span>.</span><span>.</span></span>").addClass("loading_dots");
        $("#blocks_table tbody").empty();
        $("#blocks_table").parent().addClass("data-loading").removeClass("data-empty");

        $(this).parents(".btn-group").find(".text").text($(this).text());

        $(".popover").remove();

        NRS.pageNumber = 1;

        NRS.loadPage("blocks");
    });

	return NRS;
}(NRS || {}, jQuery));
