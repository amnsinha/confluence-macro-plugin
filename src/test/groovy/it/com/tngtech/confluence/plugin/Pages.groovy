package it.com.tngtech.confluence.plugin

import geb.Page

class Confluence extends Page {
    static url = ""

    static at = {
        title == "Dashboard - Confluence"
    }
}

class EmptyPage extends Page {
    static url = "login.action"
}

class LoginPage extends Page {
    static url = "login.action"

    static at = {
        title == "Log In - Confluence" &&
                !$('div#login-container').text().contains('You are currently logged in as')

    }
    static content = {
        username { $('input', name: 'os_username') }
        password { $('input', name: 'os_password') }
        button(to: Confluence) {
            $('input', name: 'login')
        }
    }
}

// wysiwig Editor iframe
class TextArea extends Page {
    static content = {
        macro(required: false, wait: true) { $('.wysiwyg-macro') }
        macroBody(required: false, wait: true) { $('.wysiwyg-macro-body') }
        editor { $('#tinymce') }

        table(required: false, wait: true) { row, col ->
            macroBody.find('tr')[row].find('th, td')[col]
        }
    }
}

class EditPage extends Page {
    static at = {
        wysiwyg.displayed
    }

    String convertToPath(Object[] args) {
        args ? "editpage.action?pageId=" + args[0] : "createpage.action?spaceKey=TST" + args[1]
    }
    static url = "pages/"

    def waitForEditorLoaded() {
        waitFor(15.0) {
            js."AJS.MacroBrowser.metadataList.length" > 0
        }
        waitFor {
            $('#wysiwygTextarea_ifr').size() == 1
        }
    }
    static content = {
        wysiwyg(wait: true) { $('iframe#wysiwygTextarea_ifr') }
        pageTitle { $('input', name: 'title') }
        insert { $('#rte-button-insert') }
        insertWiki { $('#rte-insert-wikimarkup') }

        // insertWikiMarkkup Dialog
        textArea(required: false) { $('#insertwikitextarea') }
        insertWikiDialog(required: false) { $('div#insert-wiki-markup-dialog') }
        ok(required: false) { insertWikiDialog.find('.button-panel-button') }

        save(to: MultivotePage) { $('button#rte-button-publish') }

        // propertyPanel TODO module, page object?
        panelButtons(required: false, wait: true) { $('span.panel-button-text') }
        addEntry(required: false, wait: true) { panelButtons.filter('span', text: 'Add Entry') }
        toggleSort(required: false, wait: true) { panelButtons.filter('span', text: 'Toggle Sort') }
        reset(required: false, wait: true) { panelButtons.filter('span', text: 'Reset') }

        // insertMacroDialog
        insertMacroDialog { $('#macro-browser-dialog') }
        idParameter { $('#macro-param-id') }
        insertButton { $('.button-panel-button.ok') }
    }
}

class MultivotePage extends Page {
    String convertToPath(Object[] args) {
        "pages/viewpage.action?pageId=${args[0]}"
    }
    static url = "" // TODO not needed?

    static at = {
        true //TODO
    }

    // TODO actually check if a jquery ajax request is in progress
    def waitForAjax = {
        waitFor {
            !voteLink(1).attr('src').endsWith('roller.gif') &&
                    !voteLink(2).attr('src').endsWith('roller.gif')
        }
    }

    static content = {
        table { $('table', 'data-tableid': 'tableID' + it) }
        line { table(it).find('tbody').find('tr') }
        voteLink { line(it).find('input', id: 'multivote.1000') }
        audience { line(it).find('td', id: 'audience.1000') }
        audienceColumn { line(it).find('td.multivoteAudienceColumn') }
        edit(to: EditPage) { $('#editPageLink') }
        action { $('#action-menu-link') }

        toggleVoters { $('.multivote').find('.multivote-toggle-voters-button') }
        exportToCsv { $('.multivote').find('.multivote-export-button') }
    }
}
