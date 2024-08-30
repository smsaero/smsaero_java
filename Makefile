GPG_TTY=$(tty)
export GPG_TTY


all: release


release:
	@mvn clean deploy -P release


docker-build-and-push:
	@docker buildx create --name smsaero_java --use || docker buildx use smsaero_java
	@docker buildx build --platform linux/amd64,linux/arm64 -t 'smsaero/smsaero_java:latest' . -f Dockerfile --push
	@docker buildx rm smsaero_java
