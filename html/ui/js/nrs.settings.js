/**
 * @depends {nrs.js}
 */
var NRS = (function(NRS, $, undefined) {
	NRS.defaultSettings = {
        "language": "en",
        "hourFormat": "24h",
        "rememberPassphrase": "no",
        "itemsPerPage": "15",

		"feeWarning": "100000000000",
		"amountWarning": "100000000000"
	};

	NRS.languages = {
		"en": "English",	// english
        "cs": "Čeština",	// czech
        "es": "Español",	// spanish
        "pl": "Polski"		// polish
	};

	NRS.pages.settings = function() {
		for (var key in NRS.settings) {
			if (/Warning/i.test(key)) {
				$("#settings_" + key).val(NRS.convertToFCH(NRS.settings[key]));
			} else {
				var settingValue = NRS.settings[key];
                NRS.resetOptions(key, settingValue)
			}
		}

		NRS.pageLoaded();
	};

	NRS.resetOptions = function (key, settingValue) {
		var ul = $("#settings_" + key + "_values");

        ul.find("li").each(function (index, value) {
            var id = $(this).attr('id');
        	var text = $(this).text();

            if (settingValue === id) {
                $("#settings_" + key).text(text);
                text = "<strong>" + text + "</strong>";
			}

            $(this).empty();
            $(this).append(text);
        })
    };

	NRS.createFlagsLockscreen = function () {
		var flags = $('#language_flags');
		flags.empty();

        $.each(NRS.languages, function(code, name) {
            if (NRS.settings["language"] === code) {
                flags.append('<img id="' + code + '" style="opacity: 1;" class="flag-icon" src="../img/' + code + '.png" />');
			} else {
                flags.append('<img id="' + code + '" style="opacity: 0.5" class="flag-icon" src="../img/' + code + '.png" />');
			}
        });

        $(".flag-icon").on("click", function (e) {
			e.preventDefault();

			var id = $(this).attr("id");
			NRS.updateSettings("language", id);

            NRS.createFlagsLockscreen();
        })
    };

	NRS.applySettings = function(key) {
		if (!key || key === "language") {
			if ($.i18n.lng() !== NRS.settings["language"]) {
				$.i18n.setLng(NRS.settings["language"], null, function() {
					$("[data-i18n]").i18n();
				});
				if (key && window.localstorage) {
					window.localStorage.setItem('i18next_lng', NRS.settings["language"]);
				}
				if (NRS.inApp) {
					parent.postMessage({
						"type": "language",
						"version": NRS.settings["language"]
					}, "*");
				}
			}
		}

		if (!key || key === "itemsPerPage") {
			NRS.itemsPerPage = NRS.settings["itemsPerPage"];
		}

		if (!key || key === "rememberPassphrase") {
			if (NRS.settings["rememberPassphrase"] === "yes") {
				NRS.setCookie("rememberPassphrase", 1, 1000);
			} else {
				NRS.deleteCookie("rememberPassphrase");
			}
		}
	};

	NRS.updateSettings = function(key, value) {
		if (key) {
			NRS.settings[key] = value;

            NRS.sendRequest("updateSettings", {
                "key": key,
                "value": value
            }, function (response) {}, false);

            NRS.applySettings(key);
		}
	};

    function formatInputNumber(number) {
        var parts = number.toString().split(".");
        parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, " ");
        return parts.join(".");
    }

	$("#settings_box input[type=text]").on("input", function(e) {
		var icon = $('#' + $(this).attr("id") + 'StatusIcon');
        var text = $('#' + $(this).attr("id") + 'StatusText');

        var value = $(this).val().replace(/\s/g, '');
        var key = $(this).attr("name");

		icon.css("visibility", "visible");
        text.css("visibility", "visible");

		$(this).val(formatInputNumber(value));

        if (!setValueNotInRange(value, icon, text, $(this).attr("name"))) {
            return;
        }

		if (/Warning/i.test(key)) {
			value = NRS.convertToNQT(value);
		}

		NRS.updateSettings(key, value);
		setValueChanged(icon, text);
	});

	setValueNotInRange = function (value, icon, text, name) {
		if (value >= 0.01 && value <= 100000) {
            return true;
        }

        var textValue = $("#" + text.attr("id") + "Value");

        icon.removeClass('fa-refresh')
			.removeClass('fa-spin')
			.removeClass('fa-check')
			.addClass('fa-times')
			.css("color", "#b70000");

        text.text("Value not in range, last set value is: ")
			.css("color", "#b70000");

		textValue.text(formatInputNumber(NRS.convertToFCH(NRS.settings[name])))
			.css("color", "#b70000")
			.css("visibility", "visible");

        var timerName = "timer_" + icon.attr('id');
        clearTimeout(window[timerName]);
        window[timerName] = setTimeout(function () {
			icon.removeClass('fa-times')
				.addClass("fa-refresh")
				.addClass('fa-spin')
				.css("color", "black")
				.css("visibility", "hidden");

			text.text("Changing value...")
				.css("color", "black")
				.css("visibility", "hidden");

			textValue.css("visibility", "hidden");
        }, 3000);

        return false;
    };

    setValueChanged = function (icon, text) {
        icon.removeClass('fa-refresh')
			.removeClass('fa-spin')
			.removeClass('fa-times')
			.addClass('fa-check')
			.css("color", "#328432");

        text.text("Value sucessfully changed")
			.css("color", "#328432");

        $("#" + text.attr("id") + "Value").css("visibility", "hidden");

        var timerName = "timer_" + icon.attr('id');
        clearTimeout(window[timerName]);
        window[timerName] = setTimeout(function () {
            icon.removeClass('fa-check')
				.addClass("fa-refresh")
				.addClass('fa-spin')
				.css("color", "black")
				.css("visibility", "hidden");

            text.text("Changing value...")
				.css("color", "black")
				.css("visibility", "hidden");
        }, 1000);
    };

    $('.drop-menu').click(function () {
        $(this).attr('tabindex', 1).focus();
        $(this).toggleClass('active');
        $(this).find('.dropeddown').slideToggle(250);
    });

    $('.drop-menu').focusout(function () {
        $(this).removeClass('active');
        $(this).find('.dropeddown').slideUp(250);
    });

    $('.drop-menu .dropeddown li').click(function () {
        $(this).parents('.drop-menu').find('span').text($(this).text());

        var prefix = "settings_";
        var suffix = "_values";

        var key = $(this).parents('.drop-menu').find('ul').attr('id').slice(prefix.length, -suffix.length);
        var value = $(this).attr('id');

        setTimeout(function () {
            NRS.updateSettings(key, value);
            NRS.resetOptions(key, value);
        }, 500);
    });

	return NRS;
}(NRS || {}, jQuery));
