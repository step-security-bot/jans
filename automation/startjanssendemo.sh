#!/bin/bash
set -eo pipefail
JANS_FQDN=$1
JANS_PERSISTENCE=$2
JANS_CI_CD_RUN=$3
EXT_IP=$4
INSTALL_ISTIO=$5
HELM_DEVELOPMENT_REPO=$6
if [[ ! "$JANS_FQDN" ]]; then
  read -rp "Enter Hostname [demoexample.jans.io]:                           " JANS_FQDN
fi
if ! [[ $JANS_FQDN == *"."*"."* ]]; then
  echo "[E] Hostname provided is invalid or empty.
    Please enter a FQDN with the format demoexample.jans.io"
  exit 1
fi
if [[ ! "$JANS_PERSISTENCE" ]]; then
  read -rp "Enter persistence type [LDAP|MYSQL|PGSQL]:                            " JANS_PERSISTENCE
fi
if [[ $JANS_PERSISTENCE != "LDAP" ]] && [[ $JANS_PERSISTENCE != "MYSQL" ]] && [[ $JANS_PERSISTENCE != "PGSQL" ]]; then
  echo "[E] Incorrect entry. Please enter either LDAP, MYSQL or PGSQL"
  exit 1
fi
if [[ ! "$HELM_DEVELOPMENT_REPO" ]]; then
  HELM_DEVELOPMENT_REPO=""
fi

LOG_TARGET="FILE"
LOG_LEVEL="TRACE"
if [[ -z $JANS_CI_CD_RUN ]]; then
  LOG_TARGET="STDOUT"
  LOG_LEVEL="INFO"
fi

if [[ -z $EXT_IP ]]; then
  EXT_IP=$(dig +short myip.opendns.com @resolver1.opendns.com)
fi

wait_for_services() {
  code=404
  while [[ "$code" != "200" ]]; do
    echo "Waiting for https://${JANS_FQDN}/$1 to respond with 200"
    code=$(curl -s -o /dev/null -w ''%{http_code}'' -k https://"${JANS_FQDN}"/"$1")
    sleep 5
  done
}

sudo apt-get update
sudo apt-get install openssl -y
sudo apt-get install python3-pip -y
sudo pip3 install pip --upgrade
sudo pip3 install setuptools --upgrade
sudo pip3 install pyOpenSSL --upgrade
sudo apt-get update
sudo apt-get install build-essential unzip -y
sudo pip3 install requests --upgrade
sudo pip3 install shiv
sudo snap install microk8s --classic
sudo microk8s.status --wait-ready
sudo microk8s.enable dns registry ingress hostpath-storage
sudo microk8s kubectl get daemonset.apps/nginx-ingress-microk8s-controller -n ingress -o yaml | sed -s "s@ingress-class=public@ingress-class=nginx@g" | microk8s kubectl apply -f -
sudo apt-get update
sudo apt-get install apt-transport-https ca-certificates curl gnupg-agent software-properties-common -y
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt-get update
sudo apt-get install net-tools
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3
chmod 700 get_helm.sh
./get_helm.sh
sudo apt-get install docker-ce docker-ce-cli containerd.io -y
sudo microk8s config | sudo tee ~/.kube/config > /dev/null
sudo snap alias microk8s.kubectl kubectl
KUBECONFIG=~/.kube/config
sudo microk8s.kubectl create namespace jans --kubeconfig="$KUBECONFIG" || echo "namespace exists"

if [[ $INSTALL_ISTIO == "true" ]]; then
  sudo microk8s.kubectl label ns jans istio-injection=enabled
  sudo curl -L https://istio.io/downloadIstio | sh -
  cd istio-*
  export PATH=$PWD/bin:$PATH
  sudo ./bin/istioctl install --set profile=demo -y
  cd ..
fi

PERSISTENCE_TYPE="sql"
if [[ $JANS_PERSISTENCE == "MYSQL" ]]; then
  sudo microk8s.kubectl get po --kubeconfig="$KUBECONFIG"
  sudo helm install my-release --set auth.rootPassword=Test1234#,auth.database=jans -n jans oci://registry-1.docker.io/bitnamicharts/mysql --kubeconfig="$KUBECONFIG"
  cat << EOF > override.yaml
config:
  countryCode: US
  email: support@gluu.org
  orgName: Gluu
  city: Austin
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 3306
    cnSqlDbDialect: mysql
    cnSqlDbHost: my-release-mysql.jans.svc
    cnSqlDbUser: root
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
EOF
fi

if [[ $JANS_PERSISTENCE == "PGSQL" ]]; then
  sudo microk8s.kubectl get po --kubeconfig="$KUBECONFIG"
  sudo helm install my-release --set auth.postgresPassword=Test1234#,auth.database=jans -n jans oci://registry-1.docker.io/bitnamicharts/postgresql --kubeconfig="$KUBECONFIG"
  cat << EOF > override.yaml
config:
  countryCode: US
  email: support@gluu.org
  orgName: Gluu
  city: Austin
  configmap:
    cnSqlDbName: jans
    cnSqlDbPort: 5432
    cnSqlDbDialect: pgsql
    cnSqlDbHost: my-release-postgresql.jans.svc
    cnSqlDbUser: postgres
    cnSqlDbTimezone: UTC
    cnSqldbUserPassword: Test1234#
EOF
fi

ENABLE_LDAP="false"
if [[ $JANS_PERSISTENCE == "LDAP" ]]; then
  openssl req \
    -x509 \
    -newkey rsa:2048 \
    -sha256 \
    -days 365 \
    -nodes \
    -keyout opendj.key \
    -out opendj.crt \
    -subj "/CN=$JANS_FQDN" \
    -addext 'subjectAltName=DNS:ldap,DNS:opendj'

  LDAP_CERT_B64=$(base64 opendj.crt -w0)
  LDAP_KEY_B64=$(base64 opendj.key -w0)

  rm -f opendj.crt opendj.key

  cat << EOF > override.yaml
config:
  countryCode: US
  email: support@gluu.org
  orgName: Gluu
  city: Austin
  configmap:
    cnLdapCrt: $LDAP_CERT_B64
    cnLdapKey: $LDAP_KEY_B64
EOF
  PERSISTENCE_TYPE="ldap"
  ENABLE_LDAP="true"
fi

echo "$EXT_IP $JANS_FQDN" | sudo tee -a /etc/hosts > /dev/null
cat << EOF >> override.yaml
global:
  cloud:
    testEnviroment: true
  istio:
    enable: $INSTALL_ISTIO
  cnPersistenceType: $PERSISTENCE_TYPE
  auth-server-key-rotation:
    enabled: true
  auth-server:
    appLoggers:
      authLogTarget: "$LOG_TARGET"
      authLogLevel: "$LOG_LEVEL"
      httpLogTarget: "$LOG_TARGET"
      httpLogLevel: "$LOG_LEVEL"
      persistenceLogTarget: "$LOG_TARGET"
      persistenceLogLevel: "$LOG_LEVEL"
      persistenceDurationLogTarget: "$LOG_TARGET"
      persistenceDurationLogLevel: "$LOG_LEVEL"
      ldapStatsLogTarget: "$LOG_TARGET"
      ldapStatsLogLevel: "$LOG_LEVEL"
      scriptLogTarget: "$LOG_TARGET"
      scriptLogLevel: "$LOG_LEVEL"
      auditStatsLogTarget: "$LOG_TARGET"
      auditStatsLogLevel: "$LOG_LEVEL"
  casa:
    # -- App loggers can be configured to define where the logs will be redirected to and the level of each in which it should be displayed.
    appLoggers:
      casaLogTarget: "$LOG_TARGET"
      casaLogLevel: "$LOG_LEVEL"
      timerLogTarget: "$LOG_TARGET"
      timerLogLevel: "$LOG_LEVEL"
    ingress:
      casaEnabled: true     
  config-api:
    appLoggers:
      configApiLogTarget: "$LOG_TARGET"
      configApiLogLevel: "$LOG_LEVEL"
  fido2:
    ingress:
      fido2ConfigEnabled: true
    appLoggers:
      fido2LogTarget: "$LOG_TARGET"
      fido2LogLevel: "$LOG_LEVEL"
      persistenceLogTarget: "$LOG_TARGET"
      persistenceLogLevel: "$LOG_LEVEL"
  scim:
    ingress:
      scimConfigEnabled: true
      scimEnabled: true
    appLoggers:
      scimLogTarget: "$LOG_TARGET"
      scimLogLevel: "$LOG_LEVEL"
      persistenceLogTarget: "$LOG_TARGET"
      persistenceLogLevel: "$LOG_LEVEL"
      persistenceDurationLogTarget: "$LOG_TARGET"
      persistenceDurationLogLevel: "$LOG_LEVEL"
      ldapStatsLogTarget: "$LOG_TARGET"
      ldapStatsLogLevel: "$LOG_LEVEL"
      scriptLogTarget: "$LOG_TARGET"
      scriptLogLevel: "$LOG_LEVEL"
  fqdn: $JANS_FQDN
  lbIp: $EXT_IP
  opendj:
    # -- Boolean flag to enable/disable the OpenDJ  chart.
    enabled: $ENABLE_LDAP
# -- Nginx ingress definitions chart
nginx-ingress:
  ingress:
    path: /
    hosts:
    - $JANS_FQDN
    # -- Secrets holding HTTPS CA cert and key.
    tls:
    - secretName: tls-certificate
      hosts:
      - $JANS_FQDN
auth-server:
  livenessProbe:
    initialDelaySeconds: 300
  readinessProbe:
    initialDelaySeconds: 300
EOF
sudo helm repo add janssen https://docs.jans.io/charts
sudo helm repo update
sudo helm install janssen janssen/janssen -n jans -f override.yaml --kubeconfig="$KUBECONFIG" "$HELM_DEVELOPMENT_REPO"

wait_for_services jans-config-api/api/v1/health/ready
wait_for_services jans-scim/sys/health-check

cat << EOF > testendpoints.sh
sudo microk8s config > config
KUBECONFIG="$PWD"/config
sleep 10
echo -e "Testing openid-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/openid-configuration
echo -e "Testing scim-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/scim-configuration
echo -e "Testing fido2-configuration endpoint.. \n"
curl -k https://$JANS_FQDN/.well-known/fido2-configuration
cd ..
EOF
sudo microk8s.kubectl -n jans wait --for=condition=available --timeout=300s deploy/janssen-auth-server --kubeconfig="$KUBECONFIG" || echo "Couldn't find deployment running tests anyways..."
sudo bash testendpoints.sh
echo -e "You may re-execute bash testendpoints.sh to do a quick test to check the openid-configuration endpoint."
