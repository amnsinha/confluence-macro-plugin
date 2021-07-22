package it.com.tngtech.confluence.plugin

import org.openqa.selenium.Keys

import java.text.SimpleDateFormat

class EditorSpec extends ConfluenceSpec {
    def format = new SimpleDateFormat("^yyyy-MM-dd'T'[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}('p'|-)[0-9]{1,2}", Locale.US);

    void createPage(text) {
        def content = confluenceRemote.convertWikiToStorageFormat(text).data.result
        def pageId = createMultivotePage(content)
        to EditPage, pageId, spaceKey
        waitForEditorLoaded()
    }

    def "toggle Sort button should toggle the sort parameter" () {
        given: 'an empty macro'
        createPage """
{multivote:newTable|id=newTable}
{multivote}"""

        expect: 'sort to be false'
        withFrame(wysiwyg, TextArea) {
            assert !($('table.wysiwyg-macro').attr('data-macro-parameters') =~ /sort=true/)
            true
        }

        when: 'toggle sort button is clicked'
        withFrame(wysiwyg, TextArea) {
            macro.click()
        }
        assert toggleSort.displayed
        toggleSort.click()

        then: 'sort is set to true'
        withFrame(wysiwyg, TextArea) {
            waitFor {
                $('table.wysiwyg-macro').attr('data-macro-parameters') =~ /sort=true/
            }
            assert $('table.wysiwyg-macro').attr('data-macro-parameters') =~ /sort=true/
            true
        }
    }

    def "macro should be initialized with sample data" () {
        given: 'an empty page'
        createPage ''

        when: 'a multivote is inserted'
        withFrame(wysiwyg, TextArea) {
            editor.click()
            editor << "{multivote"
            editor << Keys.RETURN
        }

        then: 'the add macro dialog should be displayed with a generated table id'
        insertMacroDialog.displayed
        idParameter.@value =~ /multivote_admin_\d+/

        when: 'insert is clicked'
        insertButton.click()

        then: 'the multivote will be filled with example data'
        withFrame(wysiwyg, TextArea) {
            [
                    'id': [0, 0],
                    'Usage': [0, 1],
                    'Warning': [0, 2],
                    'Add or delete columns as you please': [1, 1],
                    'The first column ("id") has to stay': [1, 2]
            ].each { text, coordinates ->
                assert table(coordinates[0], coordinates[1]).text() == text
            }
            assert table(1,0).text().matches(format.format(new Date()))
            true
        }
    }


    def "add entry on empty macro should insert an example multivote" () {
        given: 'an empty macro'
        createPage """
{multivote:newTable|id=newTable}
{multivote}
"""

        when: 'the add entry button is clicked'
        withFrame(wysiwyg, TextArea) {
            macro.click()
        }
        assert addEntry.displayed
        addEntry.click()

        then: 'an example multivote is added'
        withFrame(wysiwyg, TextArea) {
            [
                'id': [0, 0],
                'Usage': [0, 1],
                'Warning': [0, 2],
                'Add or delete columns as you please': [1, 1],
                'The first column ("id") has to stay': [1, 2]
            ].each { text, coordinates ->
                assert table(coordinates[0], coordinates[1]).text() == text
            }
            assert table(1,0).text().matches(format.format(new Date()))
            true
        }
    }

    def "add entry on filled macro should add a new line" () {
        given: 'a macro containing a table'
        createPage """
{multivote:newTable|id=newTable}
|| id || bla || blubb ||
| 2013-04-15T13:10:49.813p2 | bloerk | blibber |
{multivote}
"""
        when: 'the add entry button is clicked'
        withFrame(wysiwyg, TextArea) {
            macro.click()
        }
        assert addEntry.displayed
        addEntry.click()

        then: 'an additional line is added'
        withFrame(wysiwyg, TextArea) {
            assert table(1, 0).text() == '2013-04-15T13:10:49.813p2'

            assert table(2, 0).text() != '2013-04-15T13:10:49.813p2'
            assert table(2, 0).text().matches(format.format(new Date()))
            assert table(2, 1).text() == ""
            assert table(2, 2).text() == ""
            true
        }
    }

    def cleanup() {
        // prevent prevent navigation
        js.exec "window.onbeforeunload = function () { }"
    }
}
