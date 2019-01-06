/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	//todo: use a startForgingError function instaed!

	NRS.forms.startForgingComplete = function(response, data) {
        if ("connectionError" in response && response.connectionError) {
            $.growl($.t("error_forging_offline"), {
                "type": "danger"
            });

            $("#forging_indicator i").removeClass("fa-check").addClass('fa-times');
            $("#forging_indicator").removeClass("forging");
            $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
            $("#forging_indicator").show();
            NRS.isForging = false;

            return;
        }

        if ("versionForging" in response && !response.versionForging) {
            if ("userVersion" in response && "lastVersion" in response) {
                $.growl($.t("nrs_update_to_forge", {
                    "userVersion": response.userVersion,
                    "lastVersion": response.lastVersion
                }), {
                    "type": "danger",
                    "delay": 12000
                });
            }

            $("#forging_indicator i").removeClass("fa-check").addClass('fa-times');
            $("#forging_indicator").removeClass("forging");
            $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
            $("#forging_indicator").show();
            NRS.isForging = false;

            return;
        }

		if ("deadline" in response) {
            $("#forging_indicator i").removeClass("fa-times").addClass('fa-check');
            $("#forging_indicator").addClass("forging");
            $("#forging_indicator span").html($.t("forging")).attr("data-i18n", "forging");
            $("#forging_indicator").show();
			NRS.isForging = true;
			$.growl($.t("success_start_forging"), {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl($.t("error_start_forging"), {
				"type": 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ("logout" in data &&  data["logout"] === "true") {
            NRS.logout();
            return;
		}

        $("#forging_indicator i").removeClass("fa-check").addClass('fa-times');
        $("#forging_indicator").removeClass("forging");
        $("#forging_indicator span").html($.t("not_forging")).attr("data-i18n", "not_forging");
        $("#forging_indicator").show();
		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl($.t("success_stop_forging"), {
				"type": 'success'
			});
		} else {
			$.growl($.t("error_stop_forging"), {
				"type": 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		if (NRS.downloadingBlockchain) {
			$.growl($.t("error_forging_blockchain_downloading"), {
				"type": "danger"
			});
		} else if (NRS.state.isScanning) {
			$.growl($.t("error_forging_blockchain_rescanning"), {
				"type": "danger"
			});
		} else if (!NRS.accountInfo.publicKey) {
			$.growl($.t("error_forging_no_public_key"), {
				"type": "danger"
			});
		} else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
				$.growl($.t("error_forging_lease"), {
					"type": "danger"
				});
			} else {
				$.growl($.t("error_forging_effective_balance"), {
					"type": "danger"
				});
			}
		} else if ($(this).hasClass("forging")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	return NRS;
}(NRS || {}, jQuery));
