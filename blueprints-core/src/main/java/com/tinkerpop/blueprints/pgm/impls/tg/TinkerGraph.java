package com.tinkerpop.blueprints.pgm.impls.tg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tinkerpop.blueprints.pgm.AutomaticIndex;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Index;
import com.tinkerpop.blueprints.pgm.IndexableGraph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.impls.Parameter;
import com.tinkerpop.blueprints.pgm.impls.StringFactory;
import com.tinkerpop.blueprints.pgm.util.AutomaticIndexHelper;

/**
 * A in-memory, reference implementation of the property graph interfaces provided by Blueprints.
 * 
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class TinkerGraph implements IndexableGraph, Serializable {

	private Long currentId = 0l;
	protected Map<String, Vertex> vertices = new HashMap<String, Vertex>();
	protected Map<String, Edge> edges = new HashMap<String, Edge>();
	protected Map<String, TinkerIndex> indices = new HashMap<String, TinkerIndex>();
	protected Map<String, TinkerAutomaticIndex> autoIndices = new HashMap<String, TinkerAutomaticIndex>();
	private final String directory;
	private static final String GRAPH_FILE = "/tinkergraph.dat";

	public TinkerGraph(final String directory) {
		this.directory = directory;
		try {
			final File file = new File(directory);
			if (!file.exists()) {
				if (!file.mkdirs()) {
					throw new RuntimeException("Could not create directory.");
				}

				this.createAutomaticIndex(Index.VERTICES, TinkerVertex.class, null);
				this.createAutomaticIndex(Index.EDGES, TinkerEdge.class, null);
			} else {
				ObjectInputStream input = new ObjectInputStream(new FileInputStream(directory + GRAPH_FILE));
				TinkerGraph temp = (TinkerGraph) input.readObject();
				input.close();
				currentId = temp.currentId;
				vertices = temp.vertices;
				edges = temp.edges;
				indices = temp.indices;
				autoIndices = temp.autoIndices;
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public TinkerGraph() {
		directory = null;
		this.createAutomaticIndex(Index.VERTICES, TinkerVertex.class, null);
		this.createAutomaticIndex(Index.EDGES, TinkerEdge.class, null);
	}

	protected Iterable<TinkerAutomaticIndex> getAutoIndices() {
		return autoIndices.values();
	}

	protected Iterable<TinkerIndex> getManualIndices() {
		final HashSet<TinkerIndex> indices = new HashSet<TinkerIndex>(this.indices.values());
		indices.removeAll(autoIndices.values());
		return indices;
	}

	@Override
	public <T extends Element> AutomaticIndex<T> createAutomaticIndex(final String indexName,
			final Class<T> indexClass, Set<String> keys, final Parameter... indexParameters) {
		if (indices.containsKey(indexName)) {
			throw new RuntimeException("Index already exists: " + indexName);
		}

		final TinkerAutomaticIndex index = new TinkerAutomaticIndex(indexName, indexClass, keys);
		autoIndices.put(index.getIndexName(), index);
		indices.put(index.getIndexName(), index);
		return index;
	}

	@Override
	public <T extends Element> Index<T> createManualIndex(final String indexName, final Class<T> indexClass,
			final Parameter... indexParameters) {
		if (indices.containsKey(indexName)) {
			throw new RuntimeException("Index already exists: " + indexName);
		}

		final TinkerIndex index = new TinkerIndex(indexName, indexClass);
		indices.put(index.getIndexName(), index);
		return index;
	}

	@Override
	public <T extends Element> Index<T> getIndex(final String indexName, final Class<T> indexClass) {
		Index index = indices.get(indexName);
		if (null == index) {
			return null;
		}
		if (!indexClass.isAssignableFrom(index.getIndexClass())) {
			throw new RuntimeException(indexClass + " is not assignable from " + index.getIndexClass());
		} else {
			return index;
		}
	}

	@Override
	public Iterable<Index<? extends Element>> getIndices() {
		final List<Index<? extends Element>> list = new ArrayList<Index<? extends Element>>();
		for (Index index : indices.values()) {
			list.add(index);
		}
		return list;
	}

	@Override
	public void dropIndex(final String indexName) {
		indices.remove(indexName);
		autoIndices.remove(indexName);
	}

	@Override
	public Vertex addVertex(final Object id) {
		String idString = null;
		Vertex vertex;
		if (null != id) {
			idString = getInternalId(id);
			vertex = vertices.get(idString);
			if (null != vertex) {
				throw new RuntimeException("Vertex with id " + idString + " already exists");
			}
		} else {
			boolean done = false;
			while (!done) {
				idString = this.getNextId();
				vertex = vertices.get(idString);
				if (null == vertex) {
					done = true;
				}
			}
		}

		vertex = new TinkerVertex(idString, this);
		vertices.put(vertex.getId().toString(), vertex);
		return vertex;

	}

	protected String getInternalId(final Object id) {
		String idString;
		idString = id.toString();
		return idString;
	}

	@Override
	public Vertex getVertex(final Object id) {
		if (null == id) {
			throw new IllegalArgumentException("Element identifier cannot be null");
		}

		String idString = id.toString();
		return vertices.get(idString);
	}

	@Override
	public Edge getEdge(final Object id) {
		if (null == id) {
			throw new IllegalArgumentException("Element identifier cannot be null");
		}

		String idString = id.toString();
		return edges.get(idString);
	}

	@Override
	public Iterable<Vertex> getVertices() {
		return new LinkedList<Vertex>(vertices.values());
	}

	@Override
	public Iterable<Edge> getEdges() {
		return new LinkedList<Edge>(edges.values());
	}

	@Override
	public void removeVertex(final Vertex vertex) {
		for (Edge edge : vertex.getInEdges()) {
			this.removeEdge(edge);
		}
		for (Edge edge : vertex.getOutEdges()) {
			this.removeEdge(edge);
		}

		AutomaticIndexHelper.removeElement(this, vertex);
		for (Index index : this.getManualIndices()) {
			if (Vertex.class.isAssignableFrom(index.getIndexClass())) {
				TinkerIndex<TinkerVertex> idx = (TinkerIndex<TinkerVertex>) index;
				idx.removeElement((TinkerVertex) vertex);
			}
		}

		vertices.remove(vertex.getId().toString());
	}

	@Override
	public Edge addEdge(final Object id, final Vertex outVertex, final Vertex inVertex, final String label) {
		String idString = null;
		Edge edge;
		if (null != id) {
			idString = getInternalId(id);
			edge = edges.get(idString);
			if (null != edge) {
				throw new RuntimeException("Edge with id " + id + " already exists");
			}
		} else {
			boolean done = false;
			while (!done) {
				idString = this.getNextId();
				edge = edges.get(idString);
				if (null == edge) {
					done = true;
				}
			}
		}

		edge = new TinkerEdge(idString, outVertex, inVertex, label, this);
		edges.put(edge.getId().toString(), edge);
		final TinkerVertex out = (TinkerVertex) outVertex;
		final TinkerVertex in = (TinkerVertex) inVertex;
		out.addOutEdge(label, edge);
		in.addInEdge(label, edge);
		return edge;

	}

	@Override
	public void removeEdge(final Edge edge) {
		TinkerVertex outVertex = (TinkerVertex) edge.getOutVertex();
		TinkerVertex inVertex = (TinkerVertex) edge.getInVertex();
		if (null != outVertex && null != outVertex.outEdges) {
			final Set<Edge> edges = outVertex.outEdges.get(edge.getLabel());
			if (null != edges) {
				edges.remove(edge);
			}
		}
		if (null != inVertex && null != inVertex.inEdges) {
			final Set<Edge> edges = inVertex.inEdges.get(edge.getLabel());
			if (null != edges) {
				edges.remove(edge);
			}
		}

		AutomaticIndexHelper.removeElement(this, edge);
		for (Index index : this.getManualIndices()) {
			if (Edge.class.isAssignableFrom(index.getIndexClass())) {
				TinkerIndex<TinkerEdge> idx = (TinkerIndex<TinkerEdge>) index;
				idx.removeElement((TinkerEdge) edge);
			}
		}

		edges.remove(edge.getId().toString());
	}

	@Override
	public String toString() {
		if (null == directory) {
			return StringFactory.graphString(this, "vertices:" + vertices.size() + " edges:" + edges.size());
		} else {
			return StringFactory.graphString(this, "vertices:" + vertices.size() + " edges:" + edges.size()
					+ " directory:" + directory);
		}
	}

	@Override
	public void clear() {
		vertices.clear();
		edges.clear();
		indices.clear();
		autoIndices.clear();
		currentId = 0l;
		this.createAutomaticIndex(Index.VERTICES, TinkerVertex.class, null);
		this.createAutomaticIndex(Index.EDGES, TinkerEdge.class, null);
	}

	@Override
	public void shutdown() {
		if (null != directory) {
			try {
				File file = new File(directory + GRAPH_FILE);
				if (file.exists()) {
					file.delete();
				} else {
				}
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(directory + GRAPH_FILE));
				out.writeObject(this);
				out.close();
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}

	private String getNextId() {
		String idString;
		while (true) {
			idString = currentId.toString();
			currentId++;
			if (null == vertices.get(idString) || null == edges.get(idString) || currentId == Long.MAX_VALUE) {
				break;
			}
		}
		return idString;
	}

	public TinkerGraph getRawGraph() {
		return this;
	}
}
