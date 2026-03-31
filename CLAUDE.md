# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Digital Wallet proof-of-concept (TWDIW-poc). Spring Boot 4.0.3 application using Java 17 and Maven.

## Build & Run Commands

```bash
# Build (uses Maven wrapper, no local Maven install needed)
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ClassName

# Run a single test method
./mvnw test -Dtest=ClassName#methodName
```

## Architecture

- **Group ID:** `com.twdiw` — source packages should be under `com.twdiw.poc`
- **Build:** Maven with wrapper (mvnw); Maven 3.9.12
- Standard Spring Boot project layout: `src/main/java`, `src/main/resources`, `src/test/java`
