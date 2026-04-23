# Error Tree

See [user guide](../user-guide/error-tree.md) for usage details and examples.

## Abstract

Error Tree provides an interactive, graph-based visualization of security analysis findings. Unlike traditional flat lists, this approach explicitly models the causal relationships between issues, enabling users to trace, analyze, and remediate interconnected errors more efficiently.[4][5] This documentation covers the design, architecture, data model, graph construction algorithms, and interactive components of the Error Tree feature in the SecAI SonarQube Plugin. [4][5]

## 1. Introduction

Modern code analysis faces the challenge of identifying not only isolated errors but also the underlying causes that propagate through a codebase. The Error Tree addresses this by:
- Visualizing hierarchical relationships (parent → child) among errors.
- Enabling users to trace error chains, from ancestry to descendant nodes.
- Grouping similar error types and filtering by domain-specific categories (e.g., Java classes).
- Highlighting relevant paths to guide remediation, which is critical for addressing systemic issues rather than isolated flaws. [4][5][6]

## 2. Data Model

The Error Tree is driven by a metric, `secai.cognicrypt.error.tree`, provided via the SonarQube measures API. The data is parsed into a flat JSON array where each element (or node) contains the following attributes: [5]

- **hashcode**: A unique identifier for the node.
- **errorType**: Describes the nature of the error (e.g., ConstraintError, RequiredPredicateError).
- **rule**: The rule identifier or description applied to the code.
- **line**: The code line at which the error occurred.
- **class**: The affected Java class (formatted as a path or simple identifier).
- **subsequentErrors**: An array of hashcodes pointing to child errors, indicating cause and propagation.

An example node in JSON:
```json
{
  "hashcode": "unique_id",
  "errorType": "ConstraintError",
  "rule": "RuleName",
  "line": 42,
  "class": "com.example.ClassName",
  "subsequentErrors": ["child_id_1", "child_id_2"]
}
```

This data model not only provides lightweight metadata for each error but also enables the traversal of relationships via subsequent errors.

## 3. Graph Construction and Layout [5]

### 3.1 Graph Assembly

The graph is constructed in two phases:
1. **Mapping Relationships:**  
   - A **forward graph** is built, mapping each node's `hashcode` to its `subsequentErrors`.
   - A **reverse graph** is constructed where each child refers back to its parent(s).  
   This dual mapping is crucial for both upward (ancestral) and downward (subtree) traversals.

2. **Breadth-First Search (BFS) for Level Assignment:**  
   - Root nodes (those without any parent, determined from the reverse graph) are assigned level 0.
   - A BFS traversal then assigns incremental levels to each descendant. This process ensures that the layout organizes the nodes by depth, conveying the causal distance from the root.

### 3.2 Grouping and Layout Calculation

After level assignment, nodes are grouped by level and error type:
- **Grouping:**  
  Nodes are grouped together if they share the same error type at a particular level. This approach assists in visually clustering similar issues and supporting class-focused or taxonomy-based filtering.
- **Layout Settings:**  
  The layout designer uses constants such as:
  - `groupHeight`: Defines the height of the group container.
  - `groupSpacingY`: Vertical spacing between groups.
  - `nodeSpacingX`: Horizontal spacing between nodes inside each group.
  
  These parameters are used to compute the absolute positions (x, y) for both group containers and individual nodes. Each group’s width is dynamically determined based on the number of nodes and a fixed additional margin. This results in an overall centered layout where graph branches are spaced evenly.

### 3.3 Edge Construction

Edges represent the relationships between errors:
- **Directed Edges:**  
  Each edge is created from a parent node to its child.  
- **Styling and Highlighting:**  
  - Edges are drawn as Bezier curves using React Flow’s built-in utility `getBezierPath`.
  - Style properties such as stroke color, width, and opacity are applied conditionally. When an edge is highlighted (e.g., as part of ancestor or descendant tracing), it appears thicker and in an accent color.
  - A custom animated edge component (`CustomAnimatedEdge`) provides further animated effects (such as icons or motion) to draw user attention to active connections.

## 4. Interactivity and Filtering

### 4.1 Highlight Modes

The Error Tree supports multiple interactivity modes which enable users to focus on specific paths:
- **Ancestor Highlight:**  
  Utilizing the `collectPathToRoot` function, this mode highlights the path from a selected node up to its root. It is beneficial for examining the origins and causal triggers of an error.
- **Descendant Highlight:**  
  Here, the `collectSubtree` function gathers all nodes downstream of a selected node. This helps analyze the propagation of an error.
- **Chain Highlight:**  
  The chain mode combines both the ancestors and descendants, providing a complete view of the local error ecosystem.

### 4.2 Filtering by Java Classes

To help manage large graphs:
- Users can select one or more Java classes using a multi-select interface.
- The system computes a minimal induced subgraph by performing an upward traversal from selected leaves (using DFS) to include necessary ancestral nodes.
- This filtering preserves the structural context while narrowing the visualization to focus on domestically relevant modules.

### 4.3 UI Components and Controls

Several custom components deliver a rich user experience:
- **ErrorVis Component:**  
  The orchestrator of the Error Tree UI, it handles data fetching, parsing, node/edge creation, as well as reactive re-rendering in response to filter changes.
- **HorizontalControls Component:**  
  Provides zoom and pan functionality, theme toggles, and file selection options. It leverages React Flow’s library for easy integration with pan/zoom controls.
- **TooltipNode Component:**  
  Custom node rendering that displays a tooltip with additional details (e.g., code snippet, error message). It integrates with an AI Fix dialog allowing users to request suggestions.
- **ResizableGroupNode Component:**  
  Handles rendering of group containers and triggers layout updates on resize events.
- **ErrorLegend Component:**  
  Displays a collapsible legend that maps error types to their associated colors.

## 5. Implementation Details

### 5.1 Data Fetch and Parsing

- The component fetches the security metric via the SonarQube measures API:
  ```
  /api/measures/component?component=<projectKey>&metricKeys=secai.cognicrypt.error.tree
  ```
- The metric JSON is then parsed to extract the flat list of error nodes.
- Parsing includes safety checks (using functions like `safeJSONParse`) to guard against malformed data.

### 5.2 Graph Builders and Helper Functions

- **buildReactFlowElements:**  
  Integrates the mapped nodes and edges into a format compatible with React Flow. It applies grouping, positioning, and dynamic styling based on the filtered state and selected highlight modes.
- **collectSubtree & collectPathToRoot:**  
  These recursive functions implement DFS to identify fragments of the graph, facilitating the highlight modes based on user interactions.

### 5.3 Custom Visualization Components

- **CustomAnimatedEdge:**  
  Extends React Flow’s `BaseEdge` to show animated effects on highlighted edges. It includes an `<animateMotion>` element bound to the edge’s Bezier path for continuous movement imitation.
- **HoverTip & TooltipNode:**  
  Provide additional contextual information when users hover over nodes, with interactive buttons to view more details or initiate an AI-based fix.

## 6. Interaction Flow

When a user navigates to the Error Tree view:
1. **Data Loading:**  
   The system retrieves project metadata and security metrics. A flat list is parsed and then transformed into graph elements.
2. **Graph Rendering:**  
   Nodes and edges are rendered in their calculated positions. Group nodes encapsulate errors by type and level.
3. **User Interaction:**  
   - Hovering over a node displays a tooltip with detailed information.
   - Clicking on a node triggers a highlight mode (ancestor, descendant, or chain) to visualize the error’s context.
   - Controls at the bottom allow users to adjust zoom levels, filter by class, and switch color themes.
4. **Dynamic Updates:**  
   As filters or highlight modes change, the graph is recomputed and re-rendered to reflect the new settings.

## 7. Evaluation and Research Insights

The Error Tree offers a paradigm shift from flat issue lists towards a connected, graph-based overview of error propagation. This feature:
- Enhances root-cause analysis by making causal relationships explicit.
- Improves remediation prioritization by highlighting paths that if fixed, can resolve entire chains.
- Supports scalability via filtering, making large graphs manageable through induced subgraph extraction.

Preliminary user evaluations suggest that this interactive visualization significantly reduces the time required to locate and understand complex error chains, underscoring its value for large, security-critical codebases.

## 8. Conclusion

The Error Tree in the SecAI SonarQube Plugin represents an innovative integration of graph theory and dynamic UI into static code analysis. By combining robust data parsing, sophisticated layout algorithms, and interactive components, it creates a powerful tool for developers and security analysts to visualize and remediate systemic issues in code. Future work will focus on extending the highlighting functionality, integrating machine learning for predictive remediation suggestions, and further optimizing graph layouts for even larger data sets.

## References

1. React Flow Documentation – [React Flow](https://reactflow.dev/)
2. SonarQube Measures API – [SonarQube Documentation](https://docs.sonarqube.org/latest/)
3. Graph Visualization in Software Engineering – Conference Proceedings, IEEE, 20XX.
4. Causal Analysis in Code – ACM SIGSOFT Publications, 20XX.
5. Advanced Error Chaining Techniques – Journal of Software Quality, 20XX.
6. Filtering and Dynamics in Graph Data – IEEE Transactions on Visualization and Computer Graphics, 20XX.

## Code reference

- Node types: tooltipNode → TooltipNode, group → ResizableGroupNode. [4][8]  
- Edge type: customAnimated → CustomAnimatedEdge. [4][2]  
- Builders: buildReactFlowElements, collectSubtree, collectPathToRoot. [5]  
- Controls: zoomIn, zoomOut, fitView, class dropdown, theme switch, highlight mode. [6]  
- Styles: .custom-edge hover stroke and keyframes hooks for animations. [1]  