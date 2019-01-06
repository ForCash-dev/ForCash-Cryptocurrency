/**
 * @depends {nrs.js}
 */
var NRS = (function (NRS, $, undefined) {
    NRS.pages.contacts = function () {
        var rows = "";

        NRS.contacts.forEach(function (contact) {
            rows +=
                "<tr>" +
                "<td>" +
                "<a href='#' data-toggle='modal' data-target='#update_contact_modal' data-contact='" + String(contact.address).escapeHTML() + "'><i class='fa fa-pencil'></i></a>" +
                "</td>" +
                "<td>" +
                "<a>" + contact.nickname + "</a>" +
                "</td>" +
                "<td>" +
                "<a href='#' data-user='" + contact.address + "' class='user_info'>" + contact.address + "</a>" +
                "</td>" +
                "<td>" +
                "<a>" + contact.email.escapeHTML() + "</a>" +
                "</td>" +
                "<td>" +
                "<a>" + contact.description.escapeHTML() + "</a>" +
                "</td>" +
                "<td style='white-space:nowrap'>" +
                "<a class='goto-page btn btn-xs btn-default' href='#' data-page='send_coins' data-contact='" + String(contact.address).escapeHTML() + "'>" + $.t("send_fch") + "</a> " +
                "<a class='goto-page btn btn-xs btn-default' href='#' data-page='send_message' data-contact='" + String(contact.address).escapeHTML() + "'>" + $.t("message") + "</a> " +
                "<a class='btn btn-xs btn-default' href='#' data-toggle='modal' data-target='#delete_contact_modal' data-contact='" + String(contact.address).escapeHTML() + "'>" + $.t("delete") + "</a>" +
                "</td>" +
                "</tr>";
        });

        NRS.dataLoaded(rows);

        $("#contacts_table_container").show();
        $("#contact_page_database_error").hide();
    };

    NRS.forms.addContact = function ($modal) {
        var data = NRS.getFormData($modal.find("form:first"));

        data.account_id = String(data.account_id);

        if (!data.name) {
            return {
                "error": $.t("error_contact_name_required")
            };
        } else if (!data.account_id) {
            return {
                "error": $.t("error_account_id_required")
            };
        }

        if (/^\d+$/.test(data.name) || /^FCH\-/i.test(data.name)) {
            return {
                "error": $.t("error_contact_name_alpha")
            };
        }

        if (data.email && !/@/.test(data.email)) {
            return {
                "error": $.t("error_email_address")
            };
        }

        if (data.account_id.charAt(0) == '@') {
            var convertedAccountId = $modal.find("input[name=converted_account_id]").val();
            if (convertedAccountId) {
                data.account_id = convertedAccountId;
            } else {
                return {
                    "error": $.t("error_account_id")
                };
            }
        }

        if (/^FCH\-/i.test(data.account_id)) {
            data.account_rs = data.account_id;

            var address = new NxtAddress();

            if (address.set(data.account_rs)) {
                data.account = address.account_id();
            } else {
                return {
                    "error": $.t("error_account_id")
                };
            }
        } else {
            var address = new NxtAddress();

            if (address.set(data.account_id)) {
                data.account_rs = address.toString();
            } else {
                return {
                    "error": $.t("error_account_id")
                };
            }
        }

        NRS.sendRequest("getAccount", {
            "account": data.account_id
        }, function (response) {
            if (!response.errorCode) {
                if (response.account != data.account || response.accountRS != data.account_rs) {
                    return {
                        "error": $.t("error_account_id")
                    };
                }
            }
        }, false);

        var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal], .ignore)");

        var isOkay = false;
        NRS.sendRequest("saveContact", {
            "nickname": data.name,
            "address": data.account_id,
            "email": data.email,
            "description": data.description
        }, function (response) {
            switch (response.status) {
                case 0:
                    isOkay = true;
                    break;
                case 1:
                    $modal.find(".error_message").html($.t("error_contact_account_id_exists")).show();
                    break;
                case 2:
                    $modal.find(".error_message").html($.t("error_contact_name_exists")).show();
                    break;
                case 3:
                    $modal.find(".error_message").html($.t("error_contact_db")).show();
                    break;
            }

            setTimeout(function () {
                    $btn.button("reset");
                    $modal.modal("unlock");

                    $("[class*='btn-cancel']").removeAttr('disabled');

                    if (isOkay) {
                        $modal.modal("hide");
                        $.growl($.t("success_contact_add"), {
                            "type": "success"
                        });
                    }
                }
                ,
                50
            );
        }, false);

        if (isOkay) {
            NRS.sendRequest("getContacts", null, function (response) {
                if (response.contacts) {
                    NRS.contacts = response.contacts;
                }
            }, false);

            switch (NRS.currentPage) {
                case "contacts":
                    NRS.loadPage("contacts");
                    break;

                case "transactions":
                    NRS.loadPage("transactions");
                    break;

                case "dashboard":
                    NRS.loadPage("dashboard");
                    break;

                case "blocks":
                    NRS.loadPage("blocks");
                    break;

            }
        }
    };

    $("#update_contact_modal").on("show.bs.modal", function (e) {
        var $invoker = $(e.relatedTarget);

        var contactId = $invoker.data("contact");

        if (contactId) {
            NRS.sendRequest("getContact", {
                "address": contactId
            }, function (response) {
                if (response.address !== null && response.nickname !== null) {
                    $("#update_contact_id").val(response.address);
                    $("#update_contact_name").val(response.nickname);
                    $("#update_contact_email").val(response.email);
                    $("#update_contact_description").val(response.description);
                    $("#update_contact_account_id").val(response.address);
                }
            }, false);
        }
    });

    NRS.forms.updateContact = function ($modal) {
        var data = NRS.getFormData($modal.find("form:first"));

        data.contact_id = String(data.contact_id);

        if (!data.name) {
            return {
                "error": $.t("error_contact_name_required")
            };
        }

        var $btn = $modal.find("button.btn-primary:not([data-dismiss=modal])");

        var isOkay = false;
        NRS.sendRequest("updateContact", {
            "address": data.contact_id,
            "nickname": data.name,
            "email": data.email,
            "description": data.description
        }, function (response) {
            switch (response.status) {
                case 0:
                    isOkay = true;
                    break;
                case 2:
                    $modal.find(".error_message").html($.t("error_contact_name_exists")).show();
                    break;
                case 3:
                    $modal.find(".error_message").html($.t("error_contact_db")).show();
                    break;
                case 4:
                    $modal.find(".error_message").html($.t("error_contact_name_required")).show();
                    break;

                default:
                    $modal.find(".error_message").html($.t("error_contact_db")).show();
            }

            setTimeout(function () {
                $btn.button("reset");
                $modal.modal("unlock");

                $("[class*='btn-cancel']").removeAttr('disabled');

                if (isOkay) {
                    $modal.modal("hide");
                    $.growl($.t("success_contact_update"), {
                        "type": "success"
                    });
                }
            }, 50);
        }, false);

        if (isOkay) {
            NRS.sendRequest("getContacts", null, function (response) {
                if (response.contacts) {
                    NRS.contacts = response.contacts;
                }
            }, false);

            if (NRS.currentPage === "contacts") {
                NRS.loadPage("contacts");
            } else if (NRS.currentPage === "messages" && NRS.selectedContext) {
                var heading = NRS.selectedContext.find("h4.list-group-item-heading");
                if (heading.length) {
                    heading.html(data.name.escapeHTML());
                }
            }
        }
    };

    $("#delete_contact_modal").on("show.bs.modal", function (e) {
        var $invoker = $(e.relatedTarget);

        var contactId = $invoker.data("contact");

        $("#delete_contact_id").val(contactId);

        NRS.sendRequest("getContact", {
            "address": contactId
        }, function (response) {
            if (response.address !== null && response.address !== null) {
                $("#delete_contact_name").html(response.nickname.escapeHTML());
                $("#delete_contact_account_id").html(response.address.escapeHTML());
            } else {
                $("#delete_contact_modal").hide();
                $.growl($.t("error_contact_delete"), {
                    "type": "danger"
                });
            }
        }, false);
    });

    NRS.forms.deleteContact = function ($modal) {
        var id = $("#delete_contact_id").val();

        NRS.sendRequest("deleteContact", {
            "address": id
        }, function (response) {
            setTimeout(function () {
                if (response.successfull) {
                    $.growl($.t("success_contact_delete"), {
                        "type": "success"
                    });
                } else {
                    $.growl($.t("error_contact_delete"), {
                        "type": "danger"
                    });
                }
            }, 50);
        }, false);

        NRS.sendRequest("getContacts", null, function (response) {
            if (response.contacts) {
                NRS.contacts = response.contacts;
            }
        }, false);

        if (NRS.currentPage === "contacts") {
            NRS.loadPage("contacts");
        }

        return {
            "stop": true
        };
    };

    NRS.exportContacts = function() {
        if (NRS.contacts && (Object.keys(NRS.contacts).length > 0)) {
            var contacts_download = document.createElement('a');
            contacts_download.href = 'data:text/json,' + JSON.stringify( NRS.contacts );
            contacts_download.target = '_blank';
            contacts_download.download = 'forcash-wallet-contacts.json';

            document.body.appendChild(contacts_download);
            contacts_download.click();
            document.body.removeChild(contacts_download);
        } else {
            $.growl($.t("error_no_contacts_available"), {
                "type":"warning"
            }).show();
        }
    }

    $("#export_contacts_button").on("click", function() {
        NRS.exportContacts();
    });

    NRS.importContacts = function(imported_contacts) {
        $.each(imported_contacts, function(index, imported_contact) {
            NRS.sendRequest("saveContact", {
                "nickname": imported_contact.nickname,
                "address": imported_contact.address,
                "email": imported_contact.email,
                "description": imported_contact.description
            }, function (response) {
                var isOkay = false;

                switch (response.status) {
                    case 0:
                        isOkay = true;
                        break;
                    case 1:
                        $.growl(imported_contact.address + ' - ' + $.t("error_contact_account_id_exists"), {"type":"warning"}).show();
                        break;
                    case 2:
                        $.growl(imported_contact.nickname + ' - ' + $.t("error_contact_name_exists"), {"type":"warning"}).show();
                        break;
                    case 3:
                        $.growl(imported_contact.address + ' - ' + $.t("error_contact_db"), {"type":"warning"}).show();
                        break;
                }

                if (isOkay) {
                    setTimeout(function() {
                        $.growl(imported_contact.nickname + ' - ' + $.t("success_contact_add"), {
                            "type": "success"
                        });

                        if (NRS.currentPage === "contacts") {
                            NRS.loadPage("contacts");
                        } else if (NRS.currentPage === "messages" && NRS.selectedContext) {
                            var heading = NRS.selectedContext.find("h4.list-group-item-heading");
                            if (heading.length) {
                                heading.html(imported_contact.nickname.escapeHTML());
                            }
                            NRS.selectedContext.data("context", "messages_sidebar_update_context");
                        }
                    }, 50);
                }
            }, false);
        });
    };

    $("#import_contacts_button").on("click", function() {
        $("#import_contacts_button_field").click();
    });

    $("#import_contacts_button_field").css({'display':'none'});
    $("#import_contacts_button_field").on("change", function(button_event) {
        button_event.preventDefault();
        var file = $("#import_contacts_button_field")[0].files[0];
        var reader = new FileReader();
        reader.onload = function (read_event) {
            var imported_contacts = JSON.parse(read_event.target.result);
            NRS.importContacts(imported_contacts);
        };
        reader.readAsText(file);
        var button = $("#import_contacts_button_field");
        button.replaceWith( button = button.clone(true) ); // Recreate button to clean it
        return false;
    });

    return NRS;
}(NRS || {}, jQuery));
