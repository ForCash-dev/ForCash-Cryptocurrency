/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#nrs_modal").on("show.bs.modal", function(e) {
		if (NRS.fetchingModalData) {
			return;
		}

		NRS.fetchingModalData = true;

		NRS.sendRequest("getState", function(state) {
			for (var key in state) {
				var el = $("#nrs_node_state_" + key);
				if (el.length) {
					if (key.indexOf("number") != -1) {
						el.html(NRS.formatAmount(state[key]));
					} else if (key.indexOf("Memory") != -1) {
						el.html(NRS.formatVolume(state[key]));
					} else if (key == "time") {
						el.html(NRS.formatTimestamp(state[key]));
					} else {
						el.html(String(state[key]).escapeHTML());
					}
				}
			}

			$("#nrs_update_explanation").show();

			$("#nrs_modal_state").show();

			NRS.fetchingModalData = false;
		});
	});

	$("#nrs_modal").on("hide.bs.modal", function(e) {
		$(this).find("ul.nav li.active").removeClass("active");
		$("#nrs_modal_state_nav").addClass("active");
	});

	return NRS;
}(NRS || {}, jQuery));
