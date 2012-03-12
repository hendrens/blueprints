package com.tinkerpop.blueprints.pgm.util.wrappers.partition;

import java.util.HashSet;
import java.util.Set;

import com.tinkerpop.blueprints.pgm.Element;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class PartitionElement implements Element {

	protected Element rawElement;
	protected PartitionGraph graph;

	public PartitionElement(final Element rawElement, final PartitionGraph graph) {
		this.rawElement = rawElement;
		this.graph = graph;
	}

	@Override
	public void setProperty(final String key, final Object value) {
		if (!key.equals(this.graph.getPartitionKey())) {
			this.rawElement.setProperty(key, value);
		}
	}

	@Override
	public Object getProperty(final String key) {
		if (key.equals(this.graph.getPartitionKey())) {
			return null;
		}
		return this.rawElement.getProperty(key);
	}

	@Override
	public Object removeProperty(final String key) {
		if (key.equals(this.graph.getPartitionKey())) {
			return null;
		}
		return this.rawElement.removeProperty(key);
	}

	@Override
	public Set<String> getPropertyKeys() {
		final Set<String> keys = new HashSet<String>(this.rawElement.getPropertyKeys());
		keys.remove(this.graph.getPartitionKey());
		return keys;
	}

	@Override
	public Object getId() {
		return this.rawElement.getId();
	}

	@Override
	public boolean hasId(Object identifier) {
		return this.rawElement.hasId(identifier);
	}

	@Override
	public boolean equals(final Object object) {
		return null != object && this.getClass().equals(object.getClass())
				&& this.getId().equals(((Element) object).getId());
	}

	@Override
	public int hashCode() {
		return this.rawElement.hashCode();
	}

	public Element getRawElement() {
		return this.rawElement;
	}

	public String getPartition() {
		return (String) this.rawElement.getProperty(this.graph.getPartitionKey());
	}

	public void setPartition(final String partition) {
		this.rawElement.setProperty(this.graph.getPartitionKey(), partition);
	}

	@Override
	public String toString() {
		return this.rawElement.toString();
	}
}
