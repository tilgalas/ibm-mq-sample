#!/bin/bash
# -*- mode: sh -*-
# © Copyright IBM Corporation 2015, 2019
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# downloaded from
# https://raw.githubusercontent.com/ibm-messaging/mq-dev-samples/master/gettingStarted/installing-mq-ubuntu/mq-ubuntu-install.sh

# Install script for silent install of IBM MQ on Ubuntu
# Requires apt
# We need super user permissions for some steps

set -x

MQ_TAR_GZ="9.3.5.0-IBM-MQ-Advanced-for-Developers-UbuntuLinuxX64.tar.gz"

# Set the directory to download MQ Packages
MQ_PACKAGES_DOWNLOAD_DIRECTORY=/tmp

addgroup mqclient
getent group mqclient
returnCode=$?
if [ $returnCode -eq 0 ]
then
    echo "Group mqclient exists. Proceeding with install."
    echo
else
    echo "Group mqclient does not exist!"
    echo "Please visit https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-ubuntu/ to learn how to create the required group."
    exit $returnCode
fi

adduser "app" --comment "MQ Client user" --disabled-password
adduser "app" "mqclient"
chpasswd <<EOF
app:password
EOF

apt-get update
apt-get -y install bc openjdk-17-jdk maven libncurses-dev

if ! dpkg -s ibmmq-server; then

    # Download MQ Advanced from public repo
    cd ~
    # Navigating to a directory that is accessible by the user _apt (suggested is /tmp - could be replaced)
    cd ${MQ_PACKAGES_DOWNLOAD_DIRECTORY}
    wget -c https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/messaging/mqadv/$MQ_TAR_GZ
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "Download complete"
        echo
    else
        echo "wget failed. See return code: " $returnCode
        exit $returnCode
    fi

    # Unzip and extract .tar.gz file
    tar -xzf $MQ_TAR_GZ
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "File extraction complete"
        echo
    else
        echo "File extraction failed. See return code: " $returnCode
        exit $returnCode
    fi

    # Accept the license
    cd MQServer
    ./mqlicense.sh -accept
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "license accepted"
        echo
    else
        echo "license not accepted"
        exit $returnCode
    fi

    # Create a .list file to let the system add the new packages to the apt cache
    cd /etc/apt/sources.list.d
    MQ_PACKAGES_LOCATION=${MQ_PACKAGES_DOWNLOAD_DIRECTORY}/MQServer
    echo "deb [trusted=yes] file:$MQ_PACKAGES_LOCATION ./" > mq-install.list
    apt-get update
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "apt cache update succeeded."
        echo
    else
        echo "apt cache update failed! See return code: " $returnCode
        exit $returnCode
    fi

    echo "Beginning MQ install"
    apt-get install -y "ibmmq-*"
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "Install succeeded."
    else
        echo "Install failed. See return code: " $returnCode
        exit $returnCode
    fi

    echo "Checking MQ version"
    /opt/mqm/bin/dspmqver
    returnCode=$?
    if [ $returnCode -ne 0 ]
    then
        echo "Error with dspmqver. See return code: " $returnCode
        exit $returnCode
    fi

    # Delete .list file and run apt update again to clear the apt cache
    rm /etc/apt/sources.list.d/mq-install.list
    apt-get update
    returnCode=$?
    if [ $returnCode -ne 0 ]
    then
        echo "Could not delete .list file /etc/apt/sources.list.d/mq-install.list."
        echo " See return code: " $returnCode
    else
        echo "Successfully removed .list file"
    fi
fi

DEV_USER=mqdev
adduser $DEV_USER --comment "ibm mq dev user" --disabled-password
# The group "mqm" is created during the installation. Add the dev user to it
adduser $DEV_USER mqm
echo "Successfully added $DEV_USER to group mqm"

KEYDB=/var/mqm/qmgrs/QM1/ssl/key.kdb

sudo -u $DEV_USER /bin/bash -c '
set -x
cd /opt/mqm/bin
. setmqenv -s
returnCode=$?
if [ $returnCode -eq 0 ]
then
    echo "MQ environment set"
else
    echo "MQ environment not set. See return code: " $returnCode
    exit $returnCode
fi


# Create and start a queue manager
crtmqm QM1
returnCode=$?
case $returnCode in
    0)
        echo "Successfully created a queue manager"
    ;;
    8)
        echo "Queue manager already exists"
    ;;
    *)
        echo "Problem when creating a queue manager. See return code: " $returnCode
        exit $returnCode
esac
strmqm QM1
returnCode=$?
case $returnCode in
    0)
        echo "Successfully started a queue manager"
    ;;
    5)
        echo "Queue manager already started"
    ;;
    *)
        echo "Problem when starting a queue manager. See return code: " $returnCode
        exit $returnCode
esac

KEYDB='$KEYDB'

if [ ! -e $KEYDB ]; then

    runmqckm -keydb -create -db $KEYDB -pw keydbsecret -type p12 -stash

    runmqckm \
        -cert\
        -create\
        -db $KEYDB\
        -pw keydbsecret\
        -label ibmwebspheremqqm1\
        -dn "CN=$(hostname -f), OU=DCE, O=Google, C=US"\
        -san_dnsname "$(hostname -f), localhost"\
        -type p12

fi

# Download and run developer config file to create MQ objects
MQ_DEV_CONFIG=mq-dev-config.mqsc
cd $HOME
if [ ! -e $MQ_DEV_CONFIG ]; then
    wget https://raw.githubusercontent.com/ibm-messaging/mq-dev-samples/master/gettingStarted/mqsc/$MQ_DEV_CONFIG
    echo "ALTER CHANNEL(DEV.APP.SVRCONN) CHLTYPE(SVRCONN) SSLCIPH(ANY_TLS12_OR_HIGHER) SSLCAUTH(OPTIONAL)" >> $MQ_DEV_CONFIG
    echo "REFRESH SECURITY TYPE(SSL)" >> $MQ_DEV_CONFIG
    returnCode=$?
    if [ $returnCode -eq 0 ]
    then
        echo "MQSC script successfully downloaded"
    else
        echo "MQSC script download failed. See return code: " $returnCode
        exit $returnCode
    fi
fi

# Set up MQ environment from .mqsc script
runmqsc QM1 < $MQ_DEV_CONFIG
returnCode=$?
if [ $returnCode -eq 20 ]
then
    echo "error code $?"
    echo "Error running MQSC script!"
    exit $returnCode
else
    echo "Developer configuration set up"
fi

# Set up authentication for members of the "mqclient" group
setmqaut -m QM1 -t qmgr -g mqclient +connect +inq
returnCode=$?
if [ $returnCode -ne 0 ]
then
    echo "Authorisation failed. See return code: " $returnCode
    exit $returnCode
fi
setmqaut -m QM1 -n DEV.** -t queue -g mqclient +put +get +browse +inq && setmqaut -t topic -m QM1 -n DEV.** -g mqclient +pub +sub +resume
returnCode=$?
if [ $returnCode -eq 0 ]
then
    echo "Authorisation succeeded."
else
    echo "Authorisation failed. See return code: " $returnCode
    exit $returnCode
fi

# Configure and start mqweb server
cp /opt/mqm/web/mq/samp/configuration/basic_registry.xml /var/mqm/web/installations/Installation1/servers/mqweb/mqwebuser.xml
strmqweb
returnCode=$?
if [ $returnCode -eq 0 ]
then
    echo "MQWeb server started"
else
    echo "Unable to start MQWeb server. See return code: $returnCode"
    exit $returnCode
fi

setmqweb properties -k httpHost -v '\''*'\''
returnCode=$?
if [ $returnCode -eq 0 ]
then
    echo "MQWeb httpHost set succesfully"
else
    echo "Unable to set MQWeb httpHost. See return code: $returnCode"
    exit $returnCode
fi

echo
echo "Now everything is set up with the developer configuration."
echo "For details on environment variables that must be created and a simple put/get test, visit"
echo "https://developer.ibm.com/tutorials/mq-connect-app-queue-manager-ubuntu/"
echo
'

chown mqm:mqm $KEYDB
chown mqm:mqm ${KEYDB%.kdb}.sth

# Extract web server's certificate
keytool -export\
    -keystore /var/mqm/web/installations/Installation1/servers/mqweb/resources/security/key.jks\
    -storepass password\
    -alias default\
    -rfc > /usr/local/share/ca-certificates/mqweb.crt

# Extract queue manager's certificate
keytool -export\
    -keystore /var/mqm/qmgrs/QM1/ssl/key.kdb\
    -storepass keydbsecret\
    -alias ibmwebspheremqqm1\
    -rfc > /usr/local/share/ca-certificates/qmgr.crt

update-ca-certificates

exit 0
