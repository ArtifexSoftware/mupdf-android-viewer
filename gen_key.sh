#!/bin/bash
#
# Andre Ferreira <andre.dev.linux@gmail.com>
#

# Set environment variables
set -u
export JKS_CN="MC-OAuthServer"
export JKS_OU="Authorization Server"
export JKS_O="Multicode"
export JKS_L="GO"
export JKS_ST="Goiania"
export JKS_C="BR"

export KEYSTORE_ALIAS="mc-oauth-server"
export KEYSTORE_FILE_PATH="./${KEYSTORE_ALIAS}.jks"
export KEYSTORE_PASSWORD=$(openssl rand -base64 30)

# Display Key pass to get and add in application.yaml
echo ""
echo "Key Pass: ${KEYSTORE_PASSWORD}"
echo ""

# Generate KeyPair storage type JKS
keytool -genkeypair \
  -alias $KEYSTORE_ALIAS \
  -storetype JKS \
  -keyalg RSA \
  -sigalg SHA256withRSA \
  -keysize 2048 \
  -validity 3650 \
  -dname "CN=${JKS_CN}, OU=${JKS_OU}, O=${JKS_O}, L=${JKS_L}, ST=${JKS_ST}, C=${JKS_C}" \
  -storepass $KEYSTORE_PASSWORD \
  -keypass $KEYSTORE_PASSWORD \
  -keystore $KEYSTORE_FILE_PATH

# Get base64 of keystore file
#base64 -i $KEYSTORE_FILE_PATH -w 0
export $SIGNINGKEY=$(base64 "${KEYSTORE_FILE_PATH}" | tr -d '\n')
# Remove key store file
echo ""
echo "SIGNING Key: ${SIGNINGKEY}"
echo ""
echo 
if [ -f "${KEYSTORE_FILE_PATH}" ]; then
  rm -f $KEYSTORE_FILE_PATH
fi

# Unset environment variables
set +u
unset KEYSTORE_ALIAS
unset KEYSTORE_PASSWORD
unset KEYSTORE_FILE_PATH
