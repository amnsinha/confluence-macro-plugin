//package it.com.tngtech.confluence.plugin;
//
//import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
//
//public class TestStaticLinks extends AbstractConfluencePluginWebTestCase {
//    // personalize to talk
//    protected static final String ITEM_ID = "1000";
//    protected static final String LINK_ID = "multivote.";
//    protected static final String TABLE_ID1 = "tableID1";
//    protected static final String TABLE_ID2 = "tableID2";
//
//    protected static String voteLink(String tableId) {
//        return "//table[@data-tableid='"+ tableId +"']//input[@id='" + TestJavaScriptLinks.LINK_ID + TestJavaScriptLinks.ITEM_ID + "']";
//    }
//
//    static String audienceXpath(String tableId) {
//        return "//table[@data-tableid='"+ tableId +"']//td[@id='audience." + TestJavaScriptLinks.ITEM_ID + "']";
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        gotoPage("display/TST/Multivote+Macro+Test");
//
//        ensureClean();
//    }
//
//    // TODO this is not nice, we should rather create the test data from scratch every time
//    protected void ensureClean() {
//        if (voted(TestJavaScriptLinks.TABLE_ID1)) {
//            clickVoteLink(TestJavaScriptLinks.TABLE_ID1);
//        }
//        if (voted(TestJavaScriptLinks.TABLE_ID2)) {
//            clickVoteLink(TestJavaScriptLinks.TABLE_ID2);
//        }
//    }
//
//    protected void refreshPage() {
//        // not needed, clicking the link refreshes the page anyway
//    }
//
//    protected void clickVoteLink(String tableId) {
//        clickElementByXPath("//table[@data-tableid='" + tableId + "']//input[@id='" + TestJavaScriptLinks.LINK_ID + TestJavaScriptLinks.ITEM_ID + "']");
//    }
//
//    protected void assertVoted(String tableId) {
//        assertEquals("admin", getAudience(tableId));
//        assertEquals("1", getAudienceCount(tableId));
//        assertEquals(getVotedLineClass(tableId), "interested");
//    }
//
//    protected void assertNoVote(String tableId) {
//        assertEquals("", getAudience(tableId));
//        assertEquals("0", getAudienceCount(tableId));
//        assertEquals(getVotedLineClass(tableId), "notInterested");
//    }
//
//    protected boolean voted(String tableId) {
//        return !getAudienceCount(tableId).equals("0");
//    }
//
//    // TODO these are several tests in one, but the setup is quite expensive
//    // we don't have @BeforeClass, but it can be simulated with TestSuite,
//    // see http://stackoverflow.com/questions/3023091/does-junit-3-have-something-analogous-to-beforeclass
//    public void testVoting() throws InterruptedException {
//        assertNoVote(TestJavaScriptLinks.TABLE_ID1);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID2);
//
//        clickVoteLink(TestJavaScriptLinks.TABLE_ID1);
//
//        assertVoted(TestJavaScriptLinks.TABLE_ID1);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID2);
//        refreshPage();
//        assertVoted(TestJavaScriptLinks.TABLE_ID1);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID2);
//
//        clickVoteLink(TestJavaScriptLinks.TABLE_ID1);
//        Thread.sleep(1000); // TODO
//
//        assertNoVote(TestJavaScriptLinks.TABLE_ID1);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID2);
//        refreshPage();
//        assertNoVote(TestJavaScriptLinks.TABLE_ID1);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID2);
//
//
//        clickVoteLink(TestJavaScriptLinks.TABLE_ID2);
//        assertVoted(TestJavaScriptLinks.TABLE_ID2);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID1);
//        refreshPage();
//        assertVoted(TestJavaScriptLinks.TABLE_ID2);
//        assertNoVote(TestJavaScriptLinks.TABLE_ID1);
//
//    }
//
//    protected String getAudienceCount(String id) {
//        return getElementTextByXPath(TestJavaScriptLinks.audienceXpath(id));
//    }
//
//    protected String getVotedLineClass(String tableId) {
//        return getElementAttributeByXPath("//table[@data-tableid='" + tableId + "']/tbody/tr", "class");
//    }
//
//    protected String getAudience(String tableId) {
//        return getElementAttributeByXPath("//table[@data-tableid='" + tableId + "']//td[@id='audience." + TestJavaScriptLinks.ITEM_ID + "']", "title");
//    }
//}
