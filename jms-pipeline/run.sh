#!/bin/bash

set -euo pipefail
set -x

function get_metadata_value() {
    local METADATA_SVC="http://metadata.google.internal/computeMetadata/v1"
    curl -f -H 'Metadata-Flavor: Google' "$METADATA_SVC/instance/$1" ||
        curl -f -H 'Metadata-Flavor: Google' "$METADATA_SVC/project/$1"
}

REGION=$(basename $(get_metadata_value zone) | sed 's/\(.*\)-[^-]\+/\1/g')
PROJECT=$(get_metadata_value ../project/project-id)
GCP_TEMP_LOCATION=${1:?"Missing gcp temp location"}
PIPELINE_VARIANT=${2:-""}

mvn clean compile exec:java -Dexec.mainClass="com.google.dce.JmsPipeline" -Dexec.args="\
  --runner=DataflowRunner\
  --jobName=ibm-mq${PIPELINE_VARIANT:+-$PIPELINE_VARIANT}\
  --project=$PROJECT\
  --region=$REGION\
  --subnetwork=regions/${REGION}/subnetworks/$(basename $(get_metadata_value network-interfaces/0/network))\
  --gcpTempLocation=gs://${GCP_TEMP_LOCATION#gs://}\
  --streaming\
  --enableStreamingEngine\
  ${PROFILE:+--dataflowServiceOptions=enable_google_cloud_profiler}\
  --mqUsername=app\
  --mqPassword=password\
  --mqHostname=$(hostname -f)\
  --mqApiBasePath=https://$(hostname -f):9443\
  --mqApiX509Certificate='$(cat /etc/ssl/certs/mqweb.pem)'\
  --QMgrX509Certificate='$(cat /etc/ssl/certs/qmgr.pem)'\
  ${PIPELINE_VARIANT:+--pipelineVariant=$PIPELINE_VARIANT}\
"
