Definition (Matching). A matching in a graph is a set of edges no two of which share an endpoint. 

Definition (Matched Vertex). A vertex is matched if it is incident to an edge of the matching. 

Definition (Alternating Path). An alternating path alternates between edges outside the matching and edges inside the matching. 

Definition (Augmenting Path). An augmenting path is an alternating path whose endpoints are unmatched. 

Lemma (Augmentation Lemma). If a matching has an augmenting path, flipping the edges along the path produces a larger matching. 

Theorem (Berge's Theorem). A matching is maximum if and only if there is no augmenting path.