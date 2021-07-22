/*global AJS, XDate, tinyMCE, Confluence */
AJS.bind("init.rte", function() {
    "use strict";

    var $ = AJS.$,
        register = function (name, handler) {
            AJS.Confluence.PropertyPanel.Macro.registerButtonHandler(name, function(e, macroNode) {
                var $macroNode = $(macroNode),
                    macroParameters = AJS.Confluence.MacroParameterSerializer.deserialize($macroNode.attr("data-macro-parameters"));

                handler(e, $macroNode, macroParameters);
            });
        },
        generateId = function () {
            return new XDate().toString('iz').replace(/\+/, 'p');
        },
        exampleContent = function () {
            return Confluence.Templates.Multivote.exampleMultivote({ id: generateId() });
        },
        removeNotAlnum = function (input) {
            return input.replace(/\W/g, '');
        };

    AJS.MacroBrowser.setMacroJsOverride("multivote", {
            beforeParamsSet: function (selectedParams, notSelectedMacro) {
                if (selectedParams.id === undefined) {
                    var userName = $('meta[name="ajs-remote-user"]').attr('content');
                    selectedParams.id = "multivote_" + removeNotAlnum(userName) + "_" + new Date().getTime();
                }
                return selectedParams;
            },
            beforeParamsRetrieved: function (paramMap, metadata, sharedParamMap) {
                var body = AJS.MacroBrowser.dialog.activeMetadata.formDetails.body;

                if (body.content === undefined || body.content === "") {
                    body.content = exampleContent();
                }
                return paramMap;
            }
        }
    );

    register("reset", function (e, $macroNode, macroParameters) {
        var table,
            tableId,
            url,
            itemIds,
            contextPath = AJS.params.contextPath,
            pageId = AJS.params.pageId;

        if (!window.confirm(AJS.I18n.getText('com.tngtech.confluence.plugin.multivote.confirm-reset'))) {
            return;
        }

        if (contextPath === undefined) {
            contextPath = "";
        }

        table = $macroNode.find('table');
        tableId = macroParameters.id;

        itemIds = $macroNode.find('table').find('tr > td:nth-child(1)').contents();

        url = contextPath + "/rest/multivote/1.0" +
            "/page/" + pageId +
            "/table/" + tableId +
            "?" +
            $.param(itemIds.map(function (index, item) {
                return { name: "itemId", value: $(item).text() };
            }));

        $.ajax({
            type: "DELETE", dataType: "json", url: url,
            timeout: 10000,
            error: function () {
                window.alert(AJS.I18n.getText("com.tngtech.confluence.plugin.multivote.error"));
            }
        });
    });

    register("addEntry", function (e, $macroNode) {
        var table = $macroNode.find('table > tbody').filter(':first'), tr;

        if (table.length > 0) {
            tr = $('<tr></tr>');
            table.find('tr > th').each(function () {
                tr.append('<td class="confluenceTd"></td>');
            });
            table.append(tr);
            tr.find('td:first').text(generateId());
        } else {
            if ($macroNode.find('td.wysiwyg-macro-body > p').length === 0) {
                $macroNode.find('td.wysiwyg-macro-body').append('<p></p>');
            }
            $macroNode.find('td.wysiwyg-macro-body > p').after(exampleContent());
            tr = $macroNode.find('table > tbody > tr:has(td)');
        }
    });

    register("toggleSort", function (e, $macroNode, macroParameters) {
        var defaultParameter = $macroNode.attr('data-macro-default-parameter');

        if (macroParameters.sort) {
            delete macroParameters.sort;
        } else {
            macroParameters.sort = true;
        }

        AJS.Rte.getEditor().selection.select($macroNode[0]);
        AJS.Rte.BookmarkManager.storeBookmark();

        tinyMCE.confluence.MacroUtils.insertMacro({
            contentId: AJS.Confluence.Editor.getContentId(),
            macro: {
                name: $macroNode.attr('data-macro-name'),
                params: macroParameters,
                defaultParameterValue: defaultParameter ? defaultParameter.value : undefined,
                body : AJS.Rte.getEditor().serializer.serialize($macroNode.find('td.wysiwyg-macro-body')[0])
            }
        });
    });
});
