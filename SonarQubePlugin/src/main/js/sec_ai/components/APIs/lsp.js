// JavaScript client to integrate with your SonarQube SecAI plugin
// This communicates with the LSP server to open files in VS Code

class SecaiLspClient {
    constructor() {
        this.isConnected = false;
        this.connection = null;
        this.requestId = 1;
        this.pendingRequests = new Map();
    }

    /**
     * Connect to the LSP server via WebSocket
     * You'll need to add WebSocket support to the LSP server or use HTTP
     */
    async connectToLspServer(serverUrl = 'ws://localhost:8080/lsp') {
        try {
            this.connection = new WebSocket(serverUrl);

            this.connection.onopen = () => {
                console.log('Connected to SecAI LSP Server');
                this.isConnected = true;
                this.sendInitializeRequest();
            };

            this.connection.onmessage = (event) => {
                this.handleMessage(JSON.parse(event.data));
            };

            this.connection.onclose = () => {
                console.log('Disconnected from SecAI LSP Server');
                this.isConnected = false;
            };

            this.connection.onerror = (error) => {
                console.error('LSP Connection error:', error);
                this.isConnected = false;
            };

        } catch (error) {
            console.error('Failed to connect to LSP server:', error);
            throw error;
        }
    }

    /**
     * Alternative HTTP-based approach (simpler to implement)
     * Use this if WebSocket is complex for your setup
     */
    async openFileViaHttp(filePath, lineNumber = 1, columnNumber = 1, serverUrl = 'http://localhost:8080') {
        try {
            const response = await fetch(`${serverUrl}/secai/openFile`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json'
                },
                body: JSON.stringify({
                    filePath: filePath,
                    lineNumber: lineNumber,
                    columnNumber: columnNumber
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const result = await response.json();
            console.log('File opened:', result);
            return result.success;

        } catch (error) {
            console.error('Error opening file via HTTP:', error);
            this.showError('Failed to open file in VS Code: ' + error.message);
            return false;
        }
    }

    /**
     * Send initialize request to LSP server
     */
    sendInitializeRequest() {
        const initializeParams = {
            processId: null,
            clientInfo: {
                name: "SecAI SonarQube Plugin",
                version: "1.0.0"
            },
            rootUri: null,
            capabilities: {
                workspace: {
                    executeCommand: {
                        dynamicRegistration: false
                    }
                }
            }
        };

        this.sendRequest('initialize', initializeParams);
    }

    /**
     * Open file in VS Code using LSP executeCommand
     */
    async openFileInVsCode(filePath, lineNumber = 1, columnNumber = 1) {
        if (!this.isConnected) {
            console.warn('Not connected to LSP server, trying HTTP approach');
            return await this.openFileViaHttp(filePath, lineNumber, columnNumber);
        }

        try {
            const params = {
                command: 'secai.openFile',
                arguments: [filePath, lineNumber, columnNumber]
            };

            const result = await this.sendRequest('workspace/executeCommand', params);
            console.log('File opened via LSP:', result);
            return true;

        } catch (error) {
            console.error('Error opening file via LSP:', error);
            this.showError('Failed to open file in VS Code: ' + error.message);
            return false;
        }
    }

    /**
     * Open project in VS Code
     */
    async openProjectInVsCode(projectPath) {
        if (!this.isConnected) {
            console.warn('Not connected to LSP server');
            return false;
        }

        try {
            const params = {
                command: 'secai.openProject',
                arguments: [projectPath]
            };

            const result = await this.sendRequest('workspace/executeCommand', params);
            console.log('Project opened via LSP:', result);
            return true;

        } catch (error) {
            console.error('Error opening project via LSP:', error);
            this.showError('Failed to open project in VS Code: ' + error.message);
            return false;
        }
    }

    /**
     * Send LSP request
     */
    sendRequest(method, params) {
        return new Promise((resolve, reject) => {
            if (!this.isConnected || !this.connection) {
                reject(new Error('Not connected to LSP server'));
                return;
            }

            const id = this.requestId++;
            const request = {
                jsonrpc: '2.0',
                id: id,
                method: method,
                params: params
            };

            this.pendingRequests.set(id, { resolve, reject, timestamp: Date.now() });
            this.connection.send(JSON.stringify(request));

            // Set timeout for request
            setTimeout(() => {
                if (this.pendingRequests.has(id)) {
                    this.pendingRequests.delete(id);
                    reject(new Error('Request timeout'));
                }
            }, 30000); // 30 second timeout
        });
    }

    /**
     * Handle incoming LSP messages
     */
    handleMessage(message) {
        if (message.id && this.pendingRequests.has(message.id)) {
            const { resolve, reject } = this.pendingRequests.get(message.id);
            this.pendingRequests.delete(message.id);

            if (message.error) {
                reject(new Error(message.error.message));
            } else {
                resolve(message.result);
            }
        } else if (message.method) {
            // Handle notifications from server
            this.handleNotification(message);
        }
    }

    /**
     * Handle LSP notifications
     */
    handleNotification(message) {
        console.log('LSP Notification:', message);
        // Handle server notifications if needed
    }

    /**
     * Show error message to user
     */
    showError(message) {
        // Integrate with your SonarQube UI to show error
        console.error(message);

        // Example: Show toast notification
        if (typeof window !== 'undefined' && window.SonarRequest) {
            // SonarQube-specific notification
            window.alert(message);
        }
    }

    /**
     * Show success message to user  
     */
    showSuccess(message) {
        console.log(message);

        if (typeof window !== 'undefined' && window.SonarRequest) {
            window.alert(message);
        }
    }
}

// Integration with your existing SonarQube SecAI plugin
// Add this to your AI Fix page

// Global instance
const secaiLspClient = new SecaiLspClient();

/**
 * Function to call from your "Open IDE" button
 * This is what you'll integrate with your existing AI fix page
 */
async function openInIDE(issueData) {
    try {
        // Extract file path and line number from your SonarQube issue data
        const filePath = issueData.component || issueData.filePath;
        const lineNumber = issueData.line || 1;
        const columnNumber = issueData.column || 1;

        console.log(`Opening file: ${filePath} at line ${lineNumber}`);

        // Try to open the file
        const success = await secaiLspClient.openFileInVsCode(filePath, lineNumber, columnNumber);

        if (success) {
            secaiLspClient.showSuccess('File opened in VS Code successfully!');
        } else {
            secaiLspClient.showError('Failed to open file in VS Code. Make sure VS Code is installed and the LSP server is running.');
        }

        return success;

    } catch (error) {
        console.error('Error in openInIDE:', error);
        secaiLspClient.showError('Unexpected error: ' + error.message);
        return false;
    }
}

/**
 * Initialize the LSP client when the page loads
 * Call this in your AI fix page initialization
 */
function initializeSecaiLsp() {
    // Try to connect to LSP server
    secaiLspClient.connectToLspServer().catch(error => {
        console.warn('Could not connect to LSP server via WebSocket, will use HTTP fallback');
    });
}

/**
 * Example usage in your SonarQube plugin:
 * 
 * // In your AI fix page component
 * document.addEventListener('DOMContentLoaded', function() {
 *     initializeSecaiLsp();
 * });
 * 
 * // In your "Open IDE" button click handler
 * document.getElementById('openIdeButton').addEventListener('click', function() {
 *     const issueData = {
 *         filePath: '/path/to/your/file.java',
 *         line: 42,
 *         column: 1
 *     };
 *     openInIDE(issueData);
 * });
 */

// Export for module systems
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { SecaiLspClient, openInIDE, initializeSecaiLsp };
}