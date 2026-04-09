# hybrid-rbac-abac-healthcare
Simulated healthcare microservices platform implementing Hybrid RBAC-ABAC with Keycloak, OPA, and Spring Cloud.


# Hybrid RBAC-ABAC Healthcare Platform

[![Java](https://img.shields.io/badge/Java-21-orange)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](#)
[![Keycloak](https://img.shields.io/badge/IAM-Keycloak-blue)](#)
[![OPA](https://img.shields.io/badge/Policy-OPA-purple)](#)
[![Docker](https://img.shields.io/badge/Container-Docker-2496ED)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](#license)

A production-oriented microservices platform for healthcare domains, implementing **Hybrid RBAC-ABAC** authorization using **Keycloak** (identity and role management) and **Open Policy Agent (OPA)** (policy decision engine).

---

## Table of Contents
- [Overview](#overview)
- [Problem Statement](#problem-statement)
- [Key Capabilities](#key-capabilities)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
---

## Overview

This project demonstrates how to design and implement secure, scalable healthcare microservices with fine-grained access control.  
Instead of relying on static role checks only, the system combines:

- **RBAC**: role-driven permissions from IAM
- **ABAC**: context-aware decisions using domain attributes
- **Centralized policy decision** for consistency and governance

---

## Problem Statement

Traditional access control in distributed systems often fails at one of two points:
1. Too coarse-grained (role only, hardcoded checks)
2. Too fragmented (inconsistent policy logic across services)

This platform addresses both by externalizing authorization logic through OPA while preserving service autonomy and auditability.

---

## Key Capabilities

- Hybrid authorization model: **RBAC + ABAC**
- Token-based authentication and SSO via **Keycloak**
- Policy decision orchestration through **Authorization Service**
- Domain-driven microservices (patient, appointment, lab, pharmacy, billing, etc.)
- Event-driven integration with **RabbitMQ**
- Audit trail support for compliance and traceability
- API gateway enforcement and cross-cutting security controls

---

## Architecture

### High-level components

- **API Gateway (PEP)**: request entry point, token validation, routing
- **Authorization Service (PDP Orchestrator)**: policy query orchestration
- **OPA (Policy Engine)**: evaluates Rego policies
- **PIP Services**: provide contextual attributes (user, patient, domain metadata)
- **Domain Services**: healthcare business capabilities
- **Audit Service**: captures security and business events

### Logical decision path

`Client -> Gateway -> Authorization Service -> OPA -> Allow/Deny -> Domain Service`

---

## Tech Stack

### Backend
- Java 21
- Spring Boot / Spring Cloud Gateway
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA

### Security
- Keycloak (IdP / IAM)
- Open Policy Agent (Rego)
- JWT-based authentication/authorization

### Data & Messaging
- MySQL
- RabbitMQ

### Platform & Tooling
- Docker / Docker Compose
- Maven
- Next.js (frontend)

---

## Repository Structure

```text
spring-cloud-gateway/
authorization-service/
policy-service/
user-service/
patient-service/
appointment-service/
lab-service/
pharmacy-service/
billing-service/
audit-service/
notification-service/
reporting-service/
frontend/
policies/
docs/
scripts/
