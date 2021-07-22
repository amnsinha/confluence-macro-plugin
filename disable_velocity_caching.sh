#!/bin/bash

# see https://developer.atlassian.com/display/CONFDEV/Disable+Velocity+Caching
gsed -i 's/^confplugin\.resource\.loader\.cache=true$/confplugin.resource.loader.cache=false/' target/confluence/webapp/WEB-INF/classes/velocity.properties
