# Render PostgreSQL wiring

Deployments on Render should use the managed Postgres instance rather than the local Docker container. This document captures the exact connection details that Render generated for `codevision_postgres` plus the environment variables the Spring Boot backend expects.

## Connection details

| Field        | Value |
|--------------|-------|
| Hostname (internal) | `dpg-d480qabipnbc73d6felg-a` |
| Hostname (external) | `dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com` |
| Port         | `5432` |
| Database     | `codevision_postgres` |
| Username     | `codevision_postgres_user` |
| Password     | `N7f455H9K9YZxicckzibPgLF29fHU4h3` |

Use the internal hostname for services that run inside the same Render region (no TLS overhead, no egress). Use the external hostname for local development or CI jobs that need to reach the managed database over the internet.

## Spring Boot environment variables

Add the following variables to your Render service (Dashboard → Service → Environment):

| Variable | Recommended value |
|----------|-------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://dpg-d480qabipnbc73d6felg-a:5432/codevision_postgres` (internal) or `jdbc:postgresql://dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com:5432/codevision_postgres` (external) |
| `SPRING_DATASOURCE_USERNAME` | `codevision_postgres_user` |
| `SPRING_DATASOURCE_PASSWORD` | `N7f455H9K9YZxicckzibPgLF29fHU4h3` |
| `SPRING_DATASOURCE_MAX_POOL_SIZE` | `5` (keeps the pool under Render’s connection cap) |
| `SPRING_DATASOURCE_MIN_IDLE` | `1` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` (or `none` if you promote migrations separately) |

After saving the variables, trigger a redeploy so Spring Boot picks up the new datasource configuration.

## Verifying connectivity

From your laptop you can run:

```bash
PGPASSWORD=N7f455H9K9YZxicckzibPgLF29fHU4h3 \
psql -h dpg-d480qabipnbc73d6felg-a.oregon-postgres.render.com \
     -U codevision_postgres_user \
     codevision_postgres
```

Inside Render, use the internal hostname in the same command. Once the backend restarts, `/actuator/health` should report `{"status":"UP"}` and `/analyze` invocations will persist data directly in the managed database.

## Migrating existing H2 data

If you ran CodeVision before the Postgres switch, execute the migration helper once so those historical analyses land in the managed database:

```bash
export PG_URL=jdbc:postgresql://dpg-d480qabipnbc73d6felg-a:5432/codevision_postgres   # or the external hostname
export PG_USER=codevision_postgres_user
export PG_PASSWORD='N7f455H9K9YZxicckzibPgLF29fHU4h3'
mvn dependency:get -Dartifact=com.h2database:h2:2.2.224
scripts/migration/run-h2-to-postgres.sh
```

The script copies every table (project, class metadata, snapshots, API catalog, diagrams, etc.) from the on-disk H2 file into Postgres. It is safe to re-run; `ON CONFLICT DO NOTHING` ensures existing rows are left untouched. After it finishes successfully you can delete `backend/api/data/codevision.mv.db` so no secrets linger on disk.
