[![progress-banner](https://backend.codecrafters.io/progress/claude-code/3dacf132-2ca9-433f-8d24-106eafe36049)](https://app.codecrafters.io/users/jcamiloduque?r=2qF)

# AgentBox

A lightweight Java toolkit for giving AI agents basic file and shell capabilities.

## What it does

| Action | Description |
|--------|-------------|
| **Read** | Read the contents of files. |
| **Write** | Create or modify files. |
| **Bash** | Execute shell commands. |

These tools allow an AI agent to interact with the local environment and perform useful tasks beyond generating text.

## Configuration

AgentBox works with **OpenAI-compatible APIs**, allowing it to connect to different providers and compatible local or self-hosted models.

### Required environment variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | API key used to authenticate with the AI provider. |
| `OPENAI_BASE_URL` | Base URL of the OpenAI-compatible API. |
| `OPENAI_MODEL` | Model to use. |

The API base URL and model can be changed to use different OpenAI-compatible providers or local models.

## Project Goals

AgentBox is primarily a learning project focused on exploring:

- Java
- AI agents
- Tool calling
- JSON Schema
- Local system interaction
- Agent runtimes

The goal is to keep the project small and understandable while experimenting with the building blocks of AI agents.

## Future

The project is intentionally early-stage. Future iterations may expand the available tools and evolve the interaction model toward a more conversational agent experience.

## Status

**Experimental / Learning Project**

AgentBox is under active experimentation and is not considered production-ready.
