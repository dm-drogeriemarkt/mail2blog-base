#!/usr/bin/env bash
set -e
openssl aes-256-cbc -K $encrypted_e05aca184678_key -iv $encrypted_e05aca184678_iv -in cd/codesigning.asc.enc -out cd/codesigning.asc -d
gpg --fast-import cd/codesigning.asc
mvn deploy -P sign,build-extras --settings cd/mvnsettings.xml