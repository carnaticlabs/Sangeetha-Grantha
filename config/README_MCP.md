# MCP Server Configuration

This project provides Model Context Protocol (MCP) server configurations for local development tools (Docker and Postgres).

## Generated Configuration
The file `mcp-servers.json` in this directory contains the configuration needed for:
1.  **PostgreSQL**: Connects to the local `sangita_grantha` database running in Docker.
2.  **Docker**: Allows management of local containers.

## Setup Instructions

### For Claude Desktop (macOS)
1.  Open your Claude Desktop configuration file:
    ```bash
    code ~/Library/Application\ Support/Claude/claude_desktop_config.json
    ```
2.  Add or merge the contents of `config/mcp-servers.json` into the `mcpServers` object in your config file.

    **Example `claude_desktop_config.json`**:
    ```json
    {
      "mcpServers": {
        "postgres": {
          "command": "npx",
          "args": [
            "-y",
            "@modelcontextprotocol/server-postgres",
            "postgresql://postgres:postgres@localhost:5432/sangita_grantha"
          ]
        },
        "docker": {
          "command": "npx",
          "args": [
            "-y",
            "@modelcontextprotocol/server-docker"
          ]
        }
      }
    }
    ```
3.  Restart Claude Desktop.

### Option 2: Using Claude CLI (If Installed)
If you have the `claude` CLI installed (available with Claude Code), you can run:

```bash
# Add Docker MCP Server
claude mcp add-json docker '{"command": "npx", "args": ["-y", "@modelcontextprotocol/server-docker"]}'

# Add Postgres MCP Server
claude mcp add-json postgres '{"command": "npx", "args": ["-y", "@modelcontextprotocol/server-postgres", "postgresql://postgres:postgres@localhost:5432/sangita_grantha"]}'
```

### Troubleshooting
- Ensure you have `npm` and `npx` installed and available in your PATH.
- Ensure the Postgres container is running (`docker ps` should show `sangita_postgres`).
