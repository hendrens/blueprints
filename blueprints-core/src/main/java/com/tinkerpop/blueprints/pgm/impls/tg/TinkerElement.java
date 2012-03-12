package com.tinkerpop.blueprints.pgm.impls.tg;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.impls.StringFactory;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class TinkerElement implements Element, Serializable {

	protected Map<String, Object> properties = new HashMap<String, Object>();
	protected final String id;
	private final int hashCode;
	protected final TinkerGraph graph;

	protected TinkerElement(final String id, final TinkerGraph graph) {
		this.graph = graph;
		this.id = id;
		this.hashCode = id.hashCode();
	}

	@Override
	public Set<String> getPropertyKeys() {
		return this.properties.keySet();
	}

	@Override
	public Object getProperty(final String key) {
		return this.properties.get(key);
	}

	@Override
	public void setProperty(final String key, final Object value) {
		if (key.equals(StringFactory.ID) || (key.equals(StringFactory.LABEL) && this instanceof Edge)) {
			throw new RuntimeException(key + StringFactory.PROPERTY_EXCEPTION_MESSAGE);
		}

		Object oldValue = this.properties.put(key, value);
		for (TinkerAutomaticIndex index : this.graph.getAutoIndices()) {
			index.autoUpdate(key, value, oldValue, this);
		}
	}

	@Override
	public Object removeProperty(final String key) {
		Object oldValue = this.properties.remove(key);
		for (TinkerAutomaticIndex index : this.graph.getAutoIndices()) {
			index.autoRemove(key, oldValue, this);
		}
		return oldValue;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean hasId(Object identifier) {
		return this.id.equals(this.graph.getInternalId(identifier));
	}
}
