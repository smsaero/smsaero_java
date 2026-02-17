GPG_TTY=$(tty)
export GPG_TTY

GPG_KEY_NAME ?= devops
GPG_KEY_EMAIL ?= r.askarov@smsaero.ru
GPG_KEY_ID ?=
GPG_KEYSERVER ?= keys.openpgp.org
GPG_EXPORT_DIR ?= .gpg

all: release


release:
	@mvn clean deploy -P release


gpg-generate:
	@printf 'Key-Type: RSA\nKey-Length: 4096\nSubkey-Type: RSA\nSubkey-Length: 4096\nName-Real: %s\nName-Email: %s\nExpire-Date: 0\n%%no-protection\n' "$(GPG_KEY_NAME)" "$(GPG_KEY_EMAIL)" | gpg --batch --gen-key

gpg-publish:
	@test -n "$(GPG_KEY_ID)" || (echo "Error: specify GPG_KEY_ID. Example: make gpg-publish GPG_KEY_ID=26097E2F3B709D0A1ACCF1D09279C2B5E8BADC4F" && exit 1)
	@gpg --keyserver $(GPG_KEYSERVER) --send-keys $(GPG_KEY_ID)

gpg-export:
	@test -n "$(GPG_KEY_ID)" || (echo "Error: specify GPG_KEY_ID. Example: make gpg-export GPG_KEY_ID=26097E2F3B709D0A1ACCF1D09279C2B5E8BADC4F" && exit 1)
	@mkdir -p $(GPG_EXPORT_DIR)
	@gpg --armor --export $(GPG_KEY_ID) > $(GPG_EXPORT_DIR)/public-key.asc
	@gpg --armor --export-secret-keys $(GPG_KEY_ID) > $(GPG_EXPORT_DIR)/private-key.asc

gpg-import:
	@test -n "$(GPG_KEY_FILE)" || (echo "Error: specify GPG_KEY_FILE. Example: make gpg-import GPG_KEY_FILE=.gpg/private-key.asc" && exit 1)
	@gpg --batch --import $(GPG_KEY_FILE)

gpg-list:
	@gpg --list-secret-keys --keyid-format=long


docker-build-and-push:
	@docker buildx create --name smsaero_java --use || docker buildx use smsaero_java
	@docker buildx build --platform linux/amd64,linux/arm64 -t 'smsaero/smsaero_java:latest' . -f Dockerfile --push
	@docker buildx rm smsaero_java
