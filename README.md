# High-Concurrency Ticketing API (Prevenção de Overselling)

![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.4-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-Containers-blue)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Enabled-blue)

## 📋 Visão Geral

A **High-Concurrency Ticketing API** é uma solução de alta performance desenvolvida para a plataforma de ticketeria esportiva **Prime Run**. Seu objetivo principal é processar milhares de intenções de compra simultâneas durante a abertura de lotes de inscrições de corridas de rua, garantindo **integridade transacional** e **zero ocorrência de overselling** (vendas além da capacidade do evento).

---

## 🛠️ Arquitetura e Stack Tecnológica

O projeto foi construído seguindo os princípios de **Clean Code** e **Domain-Driven Design (DDD)** simplificado:

- **Linguagem & Framework:** Java 17 / Spring Boot 3.2.4
- **Persistência de Dados:** Spring Data JPA / Hibernate ORM
- **Banco de Dados:** PostgreSQL 15 (Produção) / H2 Database (Ambiente de Testes)
- **Pool de Conexões:** HikariCP otimizado para alta concorrência
- **Monitoramento & Health Checks:** Spring Boot Actuator (`/actuator/health/readiness`, `/actuator/health/liveness`)
- **Containerização:** Docker (Multi-stage build) & Docker Compose
- **Orquestração:** Kubernetes (Manifestos de Deployment, Service, ConfigMap, Secret e HPA)

---

## 🔐 Como o Pessimistic Locking Impede o Overselling

Em cenários concorrentes sem controle de trava, duas ou mais requisições simultâneas podem ler o mesmo saldo de vagas no banco de dados (ex: `availableSlots = 1`) e ambas aprovarem a inscrição, resultando em saldo negativo (`availableSlots = -1`).

Para resolver esse problema de forma definitiva no nível de banco de dados, a API utiliza **Pessimistic Locking** (`@Lock(LockModeType.PESSIMISTIC_WRITE)`):

```java
public interface RaceRepository extends JpaRepository<Race, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Race r WHERE r.id = :id")
    Optional<Race> findByIdWithPessimisticLock(@Param("id") Long id);
}
```

### Fluxo da Transação
1. **Bloqueio de Linha (`SELECT ... FOR UPDATE`):** Quando um atleta tenta se inscrever, o Spring Data JPA emite um SQL `SELECT ... FOR UPDATE` na linha da corrida correspondente.
2. **Enfileiramento de Transações:** O PostgreSQL bloqueia a linha exclusiva para a primeira transação. Qualquer outra requisição concorrente que tentar acessar a mesma corrida fica retida na fila até o encerramento (commit/rollback) da primeira transação.
3. **Validação Atômica:** Com a linha bloqueada com exclusividade, o serviço valida se `availableSlots > 0`.
   - Se sim: decrementa `availableSlots`, registra o atleta, realiza o commit e libera a trava para o próximo da fila.
   - Se não: lança `SoldOutException` (HTTP 409 Conflict) e faz o rollback.

---

## 🚀 Como Executar com Docker Compose

### Pré-requisitos
- Docker Engine & Docker Compose instalados.

### Passos para Execução
1. Clone o repositório e navegue até a pasta do projeto:
   ```bash
   git clone https://github.com/primerun/primeRunService.git
   cd primeRunService
   ```

2. Suba o ambiente completo (Aplicação + PostgreSQL):
   ```bash
   docker compose up --build -d
   ```

3. Verifique os logs da aplicação:
   ```bash
   docker compose logs -f app
   ```

4. Verifique a saúde da aplicação via Actuator:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## 📡 Endpoints e Exemplos de Requisição cURL

### 1. Criar uma Nova Corrida
**POST** `/api/races`

```bash
curl -X POST http://localhost:8080/api/races \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Maratona Internacional de São Paulo",
    "date": "2026-11-15T07:00:00",
    "totalCapacity": 100,
    "availableSlots": 100
  }'
```

**Resposta (201 Created):**
```json
{
  "id": 1,
  "name": "Maratona Internacional de São Paulo",
  "date": "2026-11-15T07:00:00",
  "totalCapacity": 100,
  "availableSlots": 100
}
```

---

### 2. Buscar Corrida por ID
**GET** `/api/races/{id}`

```bash
curl -X GET http://localhost:8080/api/races/1
```

---

### 3. Inscrever Atleta na Corrida
**POST** `/api/registrations`

```bash
curl -X POST http://localhost:8080/api/registrations \
  -H "Content-Type: application/json" \
  -d '{
    "raceId": 1,
    "athleteName": "Ayrton Senna",
    "athleteEmail": "ayrton.senna@primerun.com"
  }'
```

**Resposta de Sucesso (201 Created):**
```json
{
  "id": 1,
  "raceId": 1,
  "athleteName": "Ayrton Senna",
  "athleteEmail": "ayrton.senna@primerun.com",
  "registeredAt": "2026-09-20T17:00:00"
}
```

**Resposta de Esgotamento (409 Conflict):**
```json
{
  "timestamp": "2026-09-20T17:00:05",
  "status": 409,
  "error": "Conflict",
  "message": "Inscrições esgotadas para esta corrida.",
  "path": "/api/registrations"
}
```

---

## ☸️ Deploy no Kubernetes (`k8s/`)

Os manifestos do Kubernetes estão estruturados no diretório `/k8s`:

```text
k8s/
├── configmap.yaml   # Configurações de ambiente (Spring Profile, DB URL)
├── secret.yaml      # Credenciais codificadas em Base64
├── deployment.yaml  # Replicas, Probes de Liveness/Readiness e Recursos (CPU/RAM)
├── service.yaml     # Exposição do serviço (ClusterIP)
└── hpa.yaml         # Autoscaling baseado em uso de CPU (Min: 2, Max: 10)
```

### Aplicar os Manifestos
```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

---

## 🧪 Testes de Concorrência

O projeto possui um teste de integração dedicado (`RaceRegistrationConcurrencyTest`) que utiliza `ExecutorService` e `CountDownLatch` para disparar 100 requisições simultâneas competindo por 5 vagas disponíveis:

```bash
./mvnw test -Dtest=RegistrationServiceTest,RegistrationControllerTest
```
