from gensim.utils import simple_preprocess
import numpy as np
import networkx as nx
import ast
from gensim.models import Word2Vec

import torch
import pydot
from gensim.utils import simple_preprocess
import numpy as np
import networkx as nx
import ast
from gensim.models import Word2Vec

import torch
import torch.nn.functional as F
from torch_geometric.nn import GCNConv, global_mean_pool
from torch_geometric.utils import from_networkx
from sklearn.preprocessing import OneHotEncoder

class GCNGraphClassifier(torch.nn.Module):
    def __init__(self, in_channels, hidden_channels, out_channels):
        super().__init__()
        self.conv1 = GCNConv(in_channels, hidden_channels)
        self.conv2 = GCNConv(hidden_channels, hidden_channels)
        self.lin = torch.nn.Linear(hidden_channels, out_channels)

    def forward(self, data):
        x, edge_index, batch = data.x, data.edge_index, data.batch
        x = F.relu(self.conv1(x, edge_index))
        x = F.relu(self.conv2(x, edge_index))
        x = global_mean_pool(x, batch)  # [num_graphs, hidden_dim]
        return self.lin(x)

#known node types, for the CPG nodes
NODE_TYPES = [
    "AggregateGraphNode",
    "ExprGraphNode",
    "ImmediateGraphNode",
    "MethodGraphNode",
    "ModifierGraphNode",
    "PropertyGraphNode",
    "RefGraphNode",
    "StmtGraphNode",
    "TypeGraphNode",
    "ValueGraphNode"
]

def node_to_feature_tensor(G, model):
    x_list = []

    # Initialize and fit OneHotEncoder
    type_encoder = OneHotEncoder(sparse=False, handle_unknown='ignore')
    type_encoder.fit(np.array(NODE_TYPES).reshape(-1, 1))

    # Adapt the node attributes and load only X values (no y anymore)
    for node, attr in G.nodes(data=True):
        print(G.nodes[node])
        
        # Parse and clean "type"
        type_str = attr.get('"type"', "")
        type_val = type_encoder.transform([[type_str]])[0]
        G.nodes[node]['"type"'] = type_val

        # Parse and clean "violation"
        violation_str = str(attr.get('"violation"', '0')).strip('"')
        violation = float(violation_str)

        # Get vector from label
        label = attr.get('"label"', "")
        vector = label_to_vector(label, model)
        G.nodes[node]["vector"] = vector

        # Combine features: [type, violation, ...vector]
        features = np.concatenate([
            type_val,
            [violation],
            vector
        ])

        x_list.append(torch.tensor(features, dtype=torch.float))

    # Remove unnecessary attributes so that no strings are left within nodes
    clean_node_attributes(G)

    # Convert structure to PyG
    data = from_networkx(G, group_node_attrs=None)
    data.x = torch.stack(x_list)

    return data

def label_to_vector(label, model):
    try:
        clean_label = ast.literal_eval(label)
    except Exception:
        clean_label = label.strip('"')

    tokens = simple_preprocess(clean_label, max_len=40)
    #print(tokens)
    vectors = [model.wv[token] for token in tokens if token in model.wv]
    #print(vectors)

    if vectors:
        return np.mean(vectors, axis=0)
    else:
        return np.zeros(model.vector_size)
    
#delete all unwanted attributes for gcn
def clean_node_attributes(G):
    for node in G.nodes:
        attrs = G.nodes[node]
        # Keep only these
        cleaned = {
            "vector": attrs["vector"],
            '"type"': attrs.get('"type"', 0),
            '"violation"': attrs.get('"violation"', 0),
        }
        # Clear and replace
        G.nodes[node].clear()
        G.nodes[node].update(cleaned)

#get the dimensions of the to be loaded gcn model, so that one does not have to load them manualy
def infer_dims_from_state(state_dict):
   
    two_d = [(k, v.shape) for k, v in state_dict.items() if isinstance(v, torch.Tensor) and v.ndim == 2]
    if not two_d:
        raise RuntimeError("Could not infer dims: no 2D weights found in state_dict.")
    first_key, first_shape = two_d[0]
    last_key, last_shape = two_d[-1]
    inferred_in = first_shape[1]
    inferred_hidden = first_shape[0]
    inferred_out = last_shape[0]
    return inferred_in, inferred_hidden, inferred_out


def calculating_confidence(hashcode, dot_graph):
    # load w2v model
    model_w2v = Word2Vec.load("jimple_word2vec.model")

    # Parse DOT string to networkx
    pydot_graphs = pydot.graph_from_dot_data(dot_graph)
    if not pydot_graphs:
        raise ValueError("No graph parsed from the provided DOT string.")
    G = nx.drawing.nx_pydot.from_pydot(pydot_graphs[0]).to_undirected()

    # Build features for the single graph
    vectorized_G = node_to_feature_tensor(G, model_w2v)

    # Load Classifier
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    state = torch.load("gcn_fp_classifier.pth", map_location=device)

    inferred_in, inferred_hidden, inferred_out = infer_dims_from_state(state)

    # Feature dimension sanity check
    feature_dim = vectorized_G.x.size(1)
    if feature_dim != inferred_in:
        print(f"[WARN] Data feature dim ({feature_dim}) != model expected in_channels ({inferred_in}). "
              f"Proceeding with model dims from checkpoint.")

    model = GCNGraphClassifier(
        in_channels=inferred_in,
        hidden_channels=inferred_hidden,
        out_channels=inferred_out,
    )
    model.load_state_dict(state)
    model.to(device)
    model.eval()

    #Predict on single Graph
    with torch.no_grad():
        data = vectorized_G.to(device)
        logits = model(data)  # shape [1, C]
        probs = torch.softmax(logits, dim=-1).squeeze(0).cpu().numpy()
        pred_class = int(probs.argmax())
        predicted_class = pred_class
        probability = probs.tolist()
        print(f"prediction: {pred_class}, probs: {probs}")
        return {
            "hashcode":hashcode,
            "prediction": predicted_class,
            "probability_score": probability[1]
        }