# API

## Check Python AI connectivity

```http
GET /api/health/ai
```

No request body or authentication is required in this learning milestone.

### Success

Status: `200 OK`

```json
{
  "status": "UP",
  "message": "Hello Spring Boot from Python AI service"
}
```

### Python unavailable or deadline exceeded

Status: `503 Service Unavailable`

```json
{
  "error": "AI_SERVICE_UNAVAILABLE",
  "message": "Python AI service is unavailable"
}
```

The backend applies a three-second deadline to the gRPC call.
