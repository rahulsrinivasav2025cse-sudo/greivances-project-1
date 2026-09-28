# E-Governance Grievance Redressal & Escalation Workflow System
Spring Boot 3 + Maven + MySQL + HTML/CSS/JS

## Run
1. Install JDK 17+, Maven, and MySQL (running on port 3306).
2. Edit `src/main/resources/application.properties` and set your MySQL username/password
   (database `grievance_db` and all tables are created automatically).
3. `mvn spring-boot:run` (or open in IntelliJ and run `GrievanceApplication`).
4. Open http://localhost:8080  (add a Department first, then register grievances).

## Workflow
SUBMITTED (Level 1) -> IN_PROGRESS -> RESOLVED / CLOSED.
SLA per priority: CRITICAL 24h, HIGH 48h, MEDIUM 72h, LOW 120h. When the SLA is missed, a scheduler
auto-escalates to Level 2 (Senior Supervisor), then Level 3 (Director), each with a fresh 24h window.
Manual escalation is also available. Every action is stored in table `escalation_logs`.

## REST API
GET/POST /api/departments ; GET/PUT/DELETE /api/departments/{id}
GET(?status=)/POST /api/grievances ; GET/PUT/DELETE /api/grievances/{id}
POST /api/grievances/{id}/escalate | /status | /resolve ; GET /api/grievances/{id}/history ; GET /api/stats
