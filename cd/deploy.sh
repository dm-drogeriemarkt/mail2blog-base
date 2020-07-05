#!/usr/bin/env bash
set -e
openssl aes-256-cbc -K $encrypted_81c98acad902_key -iv $encrypted_81c98acad902_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc
mvn deploy -P sign,build-extras --settings cd/mvnsettings.xml
