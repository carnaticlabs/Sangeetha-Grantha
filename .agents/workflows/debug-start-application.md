---
description: Start the full stack application (Database, Backend, Frontend, Extraction) with log redirection for analysis.
---

1. Start the application using Docker Compose via the Makefile and redirect output to logs.

```bash
make dev > sangita_logs.txt 2>&1 &
```

2. Wait for the services to initialize. The frontend typically runs on port 5001 and backend on 8080.

3. Tail the logs to confirm startup.
```bash
tail -f sangita_logs.txt
```

4. Verify services are healthy:
```bash
curl -sf http://localhost:8080/health && echo "Backend OK"
curl -sf http://localhost:5001 > /dev/null && echo "Frontend OK"
```
