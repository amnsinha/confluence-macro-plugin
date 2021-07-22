(function () {
    "use strict";
    /*global $: false, AJS: false, Confluence: false */
    var $ = AJS.$;

    function getParameters() {
        var params;
        if (AJS.params) {
            return AJS.params;
        }

        params = {};
        $('.parameters, .hidden').find('input').each(function(index, input) {
            input = $(input);
            params[input.attr('id')] = input.attr('value');
        });
        params.contextPath = $('meta[name="confluence-context-path"]').attr('content');
        return params;
    }

    function makeDialog() {
        var dialog = new AJS.Dialog({
                width: $(window).width() * 0.8,
                height: $(window).height() * 0.8,
                id: "export-to-csv",
                closeOnOutsideClick: true
            }),
            separatorInput,
            includeHeadersInput,
            quoteCharInput,
            inputs;

        dialog.addHeader('CSV');
        dialog.addButton('Ok', function (dialog) {
            dialog.hide();
        });

        dialog.addPanel('CSV', Confluence.Templates.Multivote.csvExport(), 'panel-body');
        dialog.show();

        inputs = $('#export-to-csv').find('input');
        separatorInput = inputs.filter('[name="separator"]');
        includeHeadersInput = inputs.filter('[name="headers"]');
        // TODO actually quote?
        quoteCharInput = inputs.filter('[name="quote-character"]');

        dialog.update = function () {
            var includeHeaders = includeHeadersInput.is(':checked'),
                separator = separatorInput.val(),
                quoteChar = quoteCharInput.val(),
                quote = function (x) {
                    return quoteChar + x + quoteChar;
                },
                joinRow = function (row) {
                    return quote(row.join(quote(separator)));
                },
                lines = includeHeaders ? dialog.withHeaders : dialog.rows,
                csv = $.map(lines, joinRow).join('\n');

            $('textarea#csvtext').text(csv);
        };

        separatorInput.change(dialog.update).keyup(dialog.update);
        quoteCharInput.change(dialog.update).keyup(dialog.update);
        includeHeadersInput.change(dialog.update);

        return dialog;
    }

    function getText(column) {
        return $(column).text().trim();
    }

    function getTexts(row) {
        var columns = $(row).children('td, th').not(':last');
        return [$.map(columns, getText)];
    }

    function init() {
        var params = getParameters(),
            dialog,
            interestedLink = $("input[ id ^= 'multivote']"),
            contextPath = params.contextPath,
            getInterestImage = function (interested) {
                if (interested) {
                    return params.interestedImage;
                }
                return params.notInterestedImage;
            };

        if (contextPath === undefined) {
            contextPath = "";
        }

        interestedLink.click(function () {
            var that = $(this),
                macro = that.closest('div.multivote'),
                fieldset = macro.parent().find("fieldset"),
                pageId = fieldset.find("#multivotePageId").attr("value"),
                itemId = that.attr("id").replace(/^multivote\./, ""),
                interested = (that.attr("data-interest") === "true"),
                line = that.closest('tr'),
                votes = line.find("td[ id ^= 'audience']"),
                audience = line.find('.multivoteAudienceColumn'),
                tableId = line.parent().parent().attr("data-tableid"),
                url = [contextPath, 'rest/multivote/1.0',
                    'page', pageId,
                    'table', tableId,
                    'item', itemId].join('/') +
                    "?" + $.param({ interested: interested });

            $.ajax({
                type:"POST", dataType:"json", url: url, data: "",
                timeout:10000,
                contentType: "application/json",
                beforeSend:function () {
                    that.attr("src", params.progressImage);
                },
                error:function () {
                    that.attr("src", getInterestImage(!interested));
                    AJS.messages.error("#multivote-error-bar", { title: data.errorTitle,
                            body: data.errorMessage, fadeout: true });
                },
                success:function (data) {
                    that.attr("data-interest", !data.interested);
                    that.attr("src", getInterestImage(data.interested));
                    votes.text(data.userNo);
                    audience.html(data.htmlUsers);
                    votes.attr("title", data.users);
                    if (data.interested) {
                        line.attr("class", "interested");
                    } else {
                        line.attr("class", "notInterested");
                    }
                    if (!data.updated) {
                        AJS.messages.error("#multivote-error-bar", { title: data.errorTitle,
                                body: data.errorMessage, fadeout: true });
                    }
                }
            });

            return false;
        });

        $('table[data-tableid]').each(function (index, table) {
            var $table = $(table),
                $multivote = $table.closest('.multivote');

            $multivote.find('.multivote-toolbar').show();

            $multivote.find('.multivote-toggle-voters-button').click(function () {
                var li = $(this).closest('li');

                $table.find('.multivoteAudienceColumn').toggle();
                li.toggleClass('active');

                return false;
            });

            $multivote.find('.multivote-export-button').click(function () {
                var rows = $.map($table.find('tbody > tr'), getTexts),
                    headers = $.map($table.find('thead > tr'), getTexts);

                if (dialog === undefined) {
                    dialog = makeDialog();
                } else {
                    dialog.show();
                }

                dialog.withHeaders = headers.concat(rows);
                dialog.rows = rows;

                dialog.update();
            });
        });
    }

    if (window.ConfluenceMobile) {
        window.ConfluenceMobile.contentEventAggregator.on("displayed", init);
    } else {
        AJS.toInit(init);
    }
}());
