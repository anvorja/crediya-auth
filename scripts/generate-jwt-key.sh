#!/usr/bin/env bash
# Genera una clave privada RSA 2048 (PKCS8) en base64, lista para la variable JWT_PRIVATE_KEY.
# No guardes la salida en el repositorio: va a un gestor de secretos (AWS Secrets Manager, Vault...).
set -euo pipefail
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 2>/dev/null | base64 -w0
echo
