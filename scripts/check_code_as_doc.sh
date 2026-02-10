#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

MAVEN_REPO_LOCAL="${MAVEN_REPO_LOCAL:-.m2/repository}"

echo "[code-as-doc] Checking generated artifacts..."
python3 scripts/generate_protocol_artifacts.py --check

echo "[code-as-doc] Running contract-focused tests..."
mvn \
  -Dmaven.repo.local="$MAVEN_REPO_LOCAL" \
  -pl api,chat-domain -am test \
  -DskipTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=CPProblemReasonTests,ApiRouteChainContractTests,CpApiPropertiesValidationTests,ProblemReasonContractTests,GeneratedProtocolArtifactsContractTests,ApiVersionFilterTests,ApiWebSocketHandlerTests,ApiWsEventStoreTests,ApiWsEventPublisherTests,ApiWsPayloadMapperTests

echo "[code-as-doc] All checks passed."
