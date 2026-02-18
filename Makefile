.DEFAULT_GOAL := help

GPG_TTY=$(tty)
export GPG_TTY

GPG_KEY_NAME ?= devops
GPG_KEY_EMAIL ?= r.askarov@smsaero.ru
GPG_KEY_ID ?=
GPG_KEYSERVER ?= keys.openpgp.org
GPG_EXPORT_DIR ?= .gpg

.PHONY: help all release gpg-generate gpg-publish gpg-export gpg-import gpg-list
.PHONY: scenario scenario-compile docker-build-and-push docker-build docker-run docker-shell
.PHONY: test test-docker compat

help: ## Show this help message
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  %-25s %s\n", $$1, $$2}'

all: release ## Build and deploy release (alias for release)

release: ## Clean and deploy Maven artifact with release profile
	@mvn clean deploy -P release

gpg-generate: ## Generate new GPG key pair (RSA 4096)
	@printf 'Key-Type: RSA\nKey-Length: 4096\nSubkey-Type: RSA\nSubkey-Length: 4096\nName-Real: %s\nName-Email: %s\nExpire-Date: 0\n%%no-protection\n' "$(GPG_KEY_NAME)" "$(GPG_KEY_EMAIL)" | gpg --batch --gen-key

gpg-publish: ## Publish GPG key to keyserver (requires GPG_KEY_ID)
	@test -n "$(GPG_KEY_ID)" || (echo "Error: specify GPG_KEY_ID. Example: make gpg-publish GPG_KEY_ID=26097E2F3B709D0A1ACCF1D09279C2B5E8BADC4F" && exit 1)
	@gpg --keyserver $(GPG_KEYSERVER) --send-keys $(GPG_KEY_ID)

gpg-export: ## Export GPG keys to files (requires GPG_KEY_ID)
	@test -n "$(GPG_KEY_ID)" || (echo "Error: specify GPG_KEY_ID. Example: make gpg-export GPG_KEY_ID=26097E2F3B709D0A1ACCF1D09279C2B5E8BADC4F" && exit 1)
	@mkdir -p $(GPG_EXPORT_DIR)
	@gpg --armor --export $(GPG_KEY_ID) > $(GPG_EXPORT_DIR)/public-key.asc
	@gpg --armor --export-secret-keys $(GPG_KEY_ID) > $(GPG_EXPORT_DIR)/private-key.asc

gpg-import: ## Import GPG key from file (requires GPG_KEY_FILE)
	@test -n "$(GPG_KEY_FILE)" || (echo "Error: specify GPG_KEY_FILE. Example: make gpg-import GPG_KEY_FILE=.gpg/private-key.asc" && exit 1)
	@gpg --batch --import $(GPG_KEY_FILE)

gpg-list: ## List all secret keys
	@gpg --list-secret-keys --keyid-format=long

scenario-compile: ## Build library and install to local repository (for scenario)
	@mvn install -DskipTests -q

scenario: scenario-compile ## Run integration scenario (.env: SMSAERO_EMAIL, SMSAERO_API_KEY, PHONE_NUMBER)
	@bash -c 'set -a; [ -f .env ] && source .env; set +a; mvn -q -f tests/scenario/pom.xml exec:java -Dexec.args=""'

docker-build-and-push: ## Build multi-arch Docker image and push to registry
	@docker buildx create --name smsaero_java --use || docker buildx use smsaero_java
	@docker buildx build --platform linux/amd64,linux/arm64 -t 'smsaero/smsaero_java:latest' . -f Dockerfile --push

docker-build: ## Build Docker image with CLI application
	@docker build -t 'smsaero/smsaero_java:latest' -f Dockerfile .

docker-run: ## Run CLI in Docker (requires EMAIL, API_KEY, PHONE, MESSAGE)
	@docker run --rm smsaero/smsaero_java:latest --email "$(EMAIL)" --api_key "$(API_KEY)" --phone "$(PHONE)" --message "$(MESSAGE)"

docker-shell: ## Start interactive shell in Docker container
	@docker run -it --rm smsaero/smsaero_java:latest /bin/sh

test: ## Run unit tests locally (Maven)
	@mvn test

test-docker: ## Run unit tests in Docker (Java 11)
	@docker run --rm -v "$(CURDIR):/project" -w /project maven:3.9-eclipse-temurin-11 mvn test -q

compat: ## Run tests on Java 11, 17, 21, 25 via Docker (compatibility matrix)
	@bash compat/run.sh
