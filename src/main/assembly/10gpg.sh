#!/bin/ash
#
# If we have key's then import them
#

export GNUPGHOME=/opt/fileserver/gpg
KEYRING=$GNUPGHOME/secring.gpg

KEYS=/opt/fileserver/keys
SECRET=$KEYS/secret.gpg
PUBLIC=$KEYS/public.gpg

if [ -d "$KEYS" -a -f "$SECRET" -a -f "$PUBLIC" ]
then
    mkdir -p $GNUPGHOME
    chmod 700 $GNUPGHOME
    if [ ! -f "$KEYRING" ]
    then
        echo "Importing keys"
        gpg --import --yes "$SECRET"
        gpg --import --yes "$PUBLIC"
        gpg --list-keys

        # Notify dpkg filesystem extensions to enable signature support
        export SIGNING_ENABLED=true
    fi
fi
