Definition (Metric TSP). In metric TSP, the input is a complete weighted graph whose edge weights satisfy the triangle inequality, and the goal is a minimum-weight Hamiltonian cycle.

Definition (Minimum Spanning Tree). A minimum spanning tree is a spanning tree of minimum total edge weight.

Definition (Odd Degree Vertices). In a graph, odd degree vertices are vertices incident to an odd number of edges.

Definition (Minimum Weight Perfect Matching). A minimum weight perfect matching pairs all vertices with minimum total edge weight.

Lemma (Parity Correction). Adding a perfect matching on the odd-degree vertices of a tree makes all degrees even.

Theorem (Christofides Approximation Sketch). For metric TSP, combine a minimum spanning tree with a minimum weight perfect matching on its odd vertices, take an Euler tour, and shortcut repeated vertices to obtain a tour of cost at most 3/2 optimum.
