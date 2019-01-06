/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS, $, undefined) {
    NRS.normalVersion = {};
    NRS.isOutdated = false;

    NRS.checkAliasVersions = function () {
        if (NRS.downloadingBlockchain) {
            $("#nrs_update_explanation > span").hide();
            $("#nrs_update_explanation_blockchain_sync").show();
            return;
        }
        if (NRS.isTestNet) {
            $("#nrs_update_explanation > span").hide();
            $("#nrs_update_explanation_testnet").show();
            return;
        }

        //Get latest version nr+hash of normal version
        NRS.sendRequest("getAlias", {
            "aliasName": "version"
        }, function (response) {
            if (response.aliasURI && (response = response.aliasURI.split(" "))) {
                NRS.normalVersion.versionNr = response[0];
                NRS.normalVersion.hash = response[1];

                if (NRS.normalVersion.versionNr) {
                    NRS.checkForNewVersion();
                }
            }
        });
    }

    NRS.checkForNewVersion = function () {
        var installVersusNormal;

        if (NRS.normalVersion && NRS.normalVersion.versionNr) {
            installVersusNormal = NRS.versionCompare(NRS.state.version, NRS.normalVersion.versionNr);
        }

        $("#nrs_update_explanation > span").hide();

        $(".nrs_new_version_nr").html(NRS.normalVersion.versionNr).show();

        if (installVersusNormal && installVersusNormal === -1) {
            NRS.isOutdated = true;

            $.growl($.t("nrs_update_available") + "<br><br><a href='#' onclick='NRS.downloadClientUpdate()'>Click to update</a>", {
                "type": "danger"
            });

            $("#nrs_update").html("Outdated!").show();
            $("#nrs_update_explanation_new_release").show();
        } else {
            NRS.isOutdated = false;
            $("#nrs_update_explanation_up_to_date").show();
        }
    }

    NRS.versionCompare = function (v1, v2) {
        if (v2 == undefined) {
            return -1;
        } else if (v1 == undefined) {
            return -1;
        }

        //https://gist.github.com/TheDistantSea/8021359 (based on)
        var v1last = v1.slice(-1);
        var v2last = v2.slice(-1);

        if (v1last == 'e') {
            v1 = v1.substring(0, v1.length - 1);
        } else {
            v1last = '';
        }

        if (v2last == 'e') {
            v2 = v2.substring(0, v2.length - 1);
        } else {
            v2last = '';
        }

        var v1parts = v1.split('.');
        var v2parts = v2.split('.');

        function isValidPart(x) {
            return /^\d+$/.test(x);
        }

        if (!v1parts.every(isValidPart) || !v2parts.every(isValidPart)) {
            return NaN;
        }

        v1parts = v1parts.map(Number);
        v2parts = v2parts.map(Number);

        for (var i = 0; i < v1parts.length; ++i) {
            if (v2parts.length == i) {
                return 1;
            }
            if (v1parts[i] == v2parts[i]) {
                continue;
            } else if (v1parts[i] > v2parts[i]) {
                return 1;
            } else {
                return -1;
            }
        }

        if (v1parts.length != v2parts.length) {
            return -1;
        }

        if (v1last && v2last) {
            return 0;
        } else if (v1last) {
            return 1;
        } else if (v2last) {
            return -1;
        } else {
            return 0;
        }
    }

    NRS.supportsUpdateVerification = function () {
        if ((typeof File !== 'undefined') && !File.prototype.slice) {
            if (File.prototype.webkitSlice) {
                File.prototype.slice = File.prototype.webkitSlice;
            }

            if (File.prototype.mozSlice) {
                File.prototype.slice = File.prototype.mozSlice;
            }
        }

        // Check for the various File API support.
        if (!window.File || !window.FileReader || !window.FileList || !window.Blob || !File.prototype.slice || !window.Worker) {
            return false;
        }

        return true;
    }

    NRS.downloadClientUpdate = function () {
        NRS.downloadedVersion = NRS.normalVersion;

        NRS.sendRequest('updateClient');

        return false;
    }

    return NRS;
}(NRS || {}, jQuery));
