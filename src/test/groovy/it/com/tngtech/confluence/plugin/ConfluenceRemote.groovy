package it.com.tngtech.confluence.plugin

import groovyx.net.http.RESTClient
import org.apache.http.client.params.ClientPNames

import static groovyx.net.http.ContentType.URLENC

import static groovyx.net.http.ContentType.JSON

class ConfluenceRemote {
    def http
    def requestId = 1

    ConfluenceRemote() {
        http = new RESTClient('http://localhost:1990/confluence/rpc/json-rpc/confluenceservice-v2')
        http.auth.basic 'admin', 'admin'
        http.setContentType(JSON)
    }


    /**
     * @return the JsessionId for the cookie
     */
    def login() {
        def http = new RESTClient('http://localhost:1990/confluence/')

        http.client.params.setParameter(ClientPNames.HANDLE_REDIRECTS, false)

        def res = http.post(
                path: 'dologin.action',
                contentType: URLENC,
                body: [
                        os_username: 'admin',
                        os_password: 'admin',
                        os_destination: ''
                ]
        )

        def cookie = res.getHeaders().'Set-Cookie'
        cookie.split(';')[0].split('=')[1]
    }

    def methodMissing(String name, args) {
        synchronized (this) {
            requestId++
        }
        http.post(
                query: [os_authType: 'basic'],
                contentType: JSON,
                body: [
                        jsonrpc: '2.0',
                        id: requestId,
                        method: name,
                        params: args
                ]
        )
    }
}
