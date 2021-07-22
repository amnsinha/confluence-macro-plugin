package it.com.tngtech.confluence.plugin

import geb.spock.GebSpec
import org.openqa.selenium.Cookie

abstract class ConfluenceSpec extends GebSpec {
    static confluenceRemote
    static pageName = 0
    static spaceId = 0
    static spaceKey = ""

    def setupSpec() {
        confluenceRemote = new ConfluenceRemote()

        login()

        spaceKey = "TST" + (spaceId++)

        def res = confluenceRemote.addSpace(
                key: spaceKey,
                name: 'Test Space',
                description: 'Space for testing Multivote'
        )
        assert res.data.error == null
    }

    def cleanupSpec() {
        confluenceRemote.removeSpace(spaceKey)

        js.exec "window.onbeforeunload = function () { }"
        clearCookies()
    }

    void login() {
        to LoginPage

        username.value 'admin'
        password.value 'admin'
        button.click()
    }

    void setLoginCookie() {
        def sessionId = confluenceRemote.login();

        to EmptyPage
        Cookie cookie2 = new Cookie("JSESSIONID", sessionId, '/');
        driver.manage().deleteAllCookies();
        driver.manage().addCookie(cookie2);
    }

    def createMultivotePage(content) {
        confluenceRemote.storePage(
            space: spaceKey,
            title: 'Test Page ' + pageName++,
            content: content
        ).data.result.id
    }
}
