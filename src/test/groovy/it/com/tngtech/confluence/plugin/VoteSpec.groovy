package it.com.tngtech.confluence.plugin

class VoteSpec extends ConfluenceSpec {
    static content
    def pageId

    def setupSpec() {
        content = confluenceRemote.convertWikiToStorageFormat("""{multivote:tableID1|id=tableID1}
|| id || name || author || description ||
| 1000 | Column1 | Column2 | Column3 |
{multivote}
{multivote:tableID2|id=tableID2}
|| id || name || author || description ||
| 1000 | Column1 | Column2 | Column3 |
{multivote}""").data.result
    }

    def setup() {
        pageId = createMultivotePage(content)
        to MultivotePage, pageId
    }

    void assertVoted(linkId) {
        assert voteLink(linkId).@src.endsWith('checkbox_checked.png')
        assert audience(linkId).@title == 'admin'
        assert audience(linkId).text() == '1'
        assert line(linkId).@class.contains('interested')
    }

    void assertNotVoted(linkId) {
        assert voteLink(linkId).@src.endsWith('checkbox_unchecked.png')
        assert audience(linkId).@title == ''
        assert audience(linkId).text() == '0'
        assert line(linkId).@class.contains('notInterested')
    }


    def "clicking the checkbox votes and un-votes" () {
        expect:
        assertNotVoted(1)
        assertNotVoted(2)

        when: "we vote in the first table"
        voteLink(1).click()
        waitForAjax()

        then: "the vote is in the first table"
        assertVoted(1)

        and: "the vote is not counted in the second table"
        assertNotVoted(2)
    }

    def "clicking twice leads to no vote" () {
        expect:
        assertNotVoted(1)

        when: "we click twice"
        voteLink(1).click()
        waitForAjax()
        voteLink(1).click()
        waitForAjax()

        then: "the vote is 'not voted'"
        assertNotVoted(1)
    }

    def "votes are kept after refreshing the page" () {
        expect:
        assertNotVoted(1)

        when: "we click the vote link"
        voteLink(1).click()
        waitForAjax()
        and: "reload the Page"
        to MultivotePage, pageId

        then: "the vote is counted"
        assertVoted(1)
        and: "no vote magically appears in second table"
        assertNotVoted(2)
    }

    def "reset the vote" () {
        when: 'we vote'
        voteLink(1).click()
        voteLink(2).click()
        waitForAjax()

        then: 'a vote is counted'
        assertVoted(1)
        assertVoted(2)

        when: 'we reset the vote'
        edit.click()
        waitForEditorLoaded()

        withFrame('wysiwygTextarea_ifr', TextArea) {
            macro.click()
        }

        assert reset.displayed
        withConfirm {
            reset.click();
        }
        save.click()
        waitFor(10) { at MultivotePage }

        then: 'the first table is reset'
        assertNotVoted(1)

        and: 'the second one is not'
        assertVoted(2)
    }

    def "voter column can be toggled" () {
        when: 'the toggle-voter-button is clicked'
        toggleVoters.click()

        then: 'an empty vote column will be displayed'
        audienceColumn(1).displayed
        audienceColumn(1).text() == ""

        when: 'vote is clicked'
        voteLink(1).click()
        waitForAjax()

        then: 'the audienceColumn will display the voters'
        audienceColumn(1).text() == "admin"

        when: 'the toggle-voter-button is clicked again'
        toggleVoters.click()

        then: 'the vote column is not displayed'
        !audienceColumn(1).displayed
    }

    def "export to csv" () {
        when: 'vote is clicked'
        voteLink(1).click()
        waitForAjax()

        and: 'export is clicked'
        exportToCsv.click()

        then: 'the csv export dialog appears'
        $('#export-to-csv').displayed

        and: 'contains the data from the vote'
        $('#csvtext').@value == '"Column1";"Column2";"Column3";"1";"admin"'

        when: 'the quote character is changed'
        $('input', name: 'quote-character').value("'")

        then: 'the requested quote character is used'
        $('#csvtext').@value == "'Column1';'Column2';'Column3';'1';'admin'"

        when: 'the separator is changed'
        $('input', name: 'separator').value('/')

        then: 'the requested separator is used'
        $('#csvtext').@value == "'Column1'/'Column2'/'Column3'/'1'/'admin'"

        when: '"use headers" is ticked'
        $('input', name: 'headers').click()

        then: 'the first contains the headers'
        $('#csvtext').@value.split('\n')[0] == "'name'/'author'/'description'/'Result'/'Voters'"
    }
}
