# SecAI SonarQube Plugin API Reference

## **Base URL**
All API endpoints are accessible under the SonarQube instance with the prefix:
```
/api/secai/
```

For calls to the external flaskapp this reference will use `localhost` in place of the IP-address. This may differ for your setup.

***

## **1. AiFix Endpoint**

### **External AI Fix Service**
**Endpoint**: External service call to SecAI backend  
**URL**: `http://localhost/aifix`  
**Method**: `POST`  
**Content-Type**: `application/json`

**Description**: Sends vulnerable code snippets to the external SecAI service for AI-powered security vulnerability analysis and remediation.

**Request Body**:
```json
{
  "llm_model": "string",     // AI model to use (e.g., "gemini:gemini-2.5-flash")
  "iterations": number,      // Number of refinement iterations (typically 3)
  "code": "string",          // Vulnerable code snippet 
  "rule": "string",          // CogniCrypt rule identifier
  "msg": "string"            // Error message from static analysis
}
```

**Response Body**:
```json
{
  "CWE_references": [
    {
      "cwe": "CWE-326",
      "link": "https://cwe.mitre.org/data/definitions/326.html"
    }
  ],
  "CogniCrypt_Verified": boolean,
  "Explanation": "string",
  "Final_Secure_Code_Snippet": "string",
  "Vulnerability_name": "string"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid request parameters
- `500 Internal Server Error`: Analysis failure

***

## **2. NewFix Endpoint**

### **Enhanced Vulnerability Chain Analysis**
**Endpoint**: External service call to SecAI backend  
**URL**: `http://localhost/newfix`  
**Method**: `POST`  
**Content-Type**: `application/json`

**Description**: Handles complex, multi-layered vulnerability analysis where security issues are interconnected through preceding and subsequent errors. Used by the LSP server component for advanced code analysis.

**Request Body**:
```json
{
  "selectedNode": {
    "severity": "string",            // MEDIUM, HIGH, etc.
    "codeSnippet": "string",         // Code snippet with issue
    "errorType": "string",           // Error classification
    "rule": "string",                // Java security rule
    "message": "string",             // Detailed error message
    "precedingErrors": ["string"],   // Array of preceding error IDs
    "subsequentErrors": ["string"]   // Array of subsequent error IDs
  },
  "fullPathFromRootToBottom": [...], // Complete error chain
  "sourceCodeAnalysis": [...],       // Full source code context
  "llm_model": "string",             // AI model specification
  "iterations": number               // Processing iterations
}
```

**Response Body**:
```json
{
  "CWE_references": [...],
  "CogniCrypt_Verified": boolean,
  "Explanation": "string",
  "Final_Secure_Code_Snippet": "string", // Complete corrected source
  "Processing_Details": {
    "auto_resolved_by_dependencies": number,
    "manually_processed": number,
    "processed_errors": [...],
    "total_errors_in_trace": number,
    "total_iterations": number,
    "verified_errors": number
  },
  "Vulnerability_name": "string"
}
```

***

## **3. False Positive Management**

### **Single False Positive Score**
**Endpoint**: External service call to SecAI backend  
**URL**: `http://localhost/fp`  
**Method**: `POST`  
**Content-Type**: `application/json`

**Description:** Returns the false positive score for a single issue.

**Request Body:**
```json
{
  "hashcode": "string",           // Unique hashcode identifying the current issue
  "dot_graph": "string",          // dot graph of the CPG for the issue. May be Base64 encoded and gzipped
  "project": "string",            // Project key of the project that the issue belongs to
  "branch": "string"              // Branch name to identify the branch that the issue belongs to
}
```

**Response Body:**
```json
{
  "hashcode": "string",           // Unique hashcode identifying the current issue
  "prediction": number,           // Whether the issue is estimated to be a true positive (1) or false positive (0)
  "probabilty_score": number      // False positive score for the issue. For confidence: 1 - probability_score
}
```

### **Multiple False Positive Scores**
**Endpoint**: External service call to SecAI backend  
**URL**: `http://localhost/fpall`  
**Method**: `POST`  
**Content-Type**: `application/json`

**Description:** Returns the false positive scores for multiple issues.

**Request Body:**
```json
{
  "project": "string",            // Project key of the project that the issue belongs to
  "branch": "string",             // Branch name to identify the branch that the issue belongs to
  "lastAnalysis": number,         // Timestamp of the last analysis in milliseconds
  "errors": [                     // List of issues
    {
      "hashcode": "string",       // Unique hashcode identifying the issue
      "dot_graph": "string"       // dot graph of the CPG for the issue. May be Base64 encoded and gzipped
    },
    ...
    {
      "hashcode": "string",       // Unique hashcode identifying the issue
      "dot_graph": "string"       // dot graph of the CPG for the issue. May be Base64 encoded and gzipped
    }
  ]
}
```

**Response Body:**
```json
{
  "fp_scores": [
    {
      "hashcode": "string",       // Unique hashcode identifying the issue
      "prediction": number,       // Whether the issue is estimated to be a true positive (1) or false positive (0)
      "probabilty_score": number  // False positive score for the issue. For confidence: 1 - probability_score
    },
    ...
    {
      "hashcode": "string",       // Unique hashcode identifying the issue
      "prediction": number,       // Whether the issue is estimated to be a true positive (1) or false positive (0)
      "probabilty_score": number  // False positive score for the issue. For confidence: 1 - probability_score
    }
  ]
}
```

***

## **4. Trigger Analysis Endpoint**

### **Start CogniCrypt Analysis**
**Endpoint**: `/api/secai/triggerAnalysis`  
**Method**: `GET`  
**Response Type**: `text/event-stream` (Server-Sent Events)

**Description**: Triggers CogniCrypt static analysis for the project's JAR file and streams real-time progress updates to the client.

**Headers**:
```
Connection: keep-alive
Cache-Control: no-cache
Content-Type: text/event-stream
```

**Event Stream Format**:
```
data: [timestamp] - [progress message]
data: Analysis completed
```

**Usage Example**:
```javascript
const eventSource = new EventSource('/api/secai/triggerAnalysis');

eventSource.onmessage = (event) => {
  const parts = event.data.split(' - ');
  const processedMessage = parts.length > 1 ? parts.slice(1).join(' - ') : event.data;
  
  if (event.data.includes('Analysis completed')) {
    eventSource.close();
  }
};
```

***

## **5. Additional Endpoints**

### **Get Full SARIF Report**
**Endpoint**: `/api/secai/getFullSarifReport`  
**Method**: `GET`  
**Content-Type**: `application/json`

**Description**: Returns the complete SARIF analysis report generated by CogniCrypt.

**Success Response**:
```json
{
  // Complete SARIF JSON structure
  "version": "2.1.0",
  "runs": [...],
  // ... full SARIF content
}
```

**Error Response**:
```json
{
  "error": "SARIF file not found"
}
```

### **Run Code Generation**
**Endpoint**: `/api/secai/runCodeGeneration`  
**Method**: `POST`  
**Content-Type**: `application/x-www-form-urlencoded`

**Description**: Executes the SecAI code generation pipeline based on natural language prompts.

**Parameters**:
- `prompt` (required): Natural-language prompt for code generation

**Success Response**:
```json
{
  "code": "string" // Generated code snippet
}
```

**Error Response**:
```json
{
  "error": "string" // Error message
}
```

***

## **Integration Notes**

### **Frontend Integration**
The plugin's React frontend integrates with these APIs through:
- Direct fetch calls to external SecAI services for AI-powered analysis
- EventSource connections for real-time analysis progress
- Standard REST calls for SARIF report retrieval

### **Authentication**
- Internal SonarQube APIs use SonarQube's built-in authentication
- External SecAI service calls may require additional configuration through SonarQube settings

### **Configuration**
API endpoints can be configured through SonarQube's plugin settings, including:
- External SecAI service URLs
- LLM model preferences
- Analysis parameters
