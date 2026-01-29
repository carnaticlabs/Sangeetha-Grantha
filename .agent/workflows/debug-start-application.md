---
description: Start the full stack application (Database, Backend, Frontend) with log redirection for analysis.
---

1. Start the application using the Sangita CLI via mise and redirect output to logs.

```bash
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db > sangita_logs.txt 2>&1 &
```

2. Wait for the services to initialize. The frontend typically runs on port 5001 and backend on 8080.
// turbo
3. Tail the logs to confirm startup.
```bash
tail -f sangita_logs.txt
```
