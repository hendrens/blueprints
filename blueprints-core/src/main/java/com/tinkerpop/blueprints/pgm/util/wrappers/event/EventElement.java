package com.tinkerpop.blueprints.pgm.util.wrappers.event;

import java.util.List;
import java.util.Set;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Element;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.wrappers.event.listener.GraphChangedListener;

/**
 * An element with a GraphChangedListener attached. Those listeners are notified when changes occur to the properties of
 * the element.
 * 
 * @author Stephen Mallette
 */
public class EventElement implements Element {
	protected final Element rawElement;
	protected final List<GraphChangedListener> graphChangedListeners;

	public EventElement(final Element rawElement, final List<GraphChangedListener> graphChangedListeners) {
		this.rawElement = rawElement;
		this.graphChangedListeners = graphChangedListeners;
	}

	protected void onVertexPropertyChanged(final Vertex vertex, final String key, final Object newValue) {
		for (GraphChangedListener listener : this.graphChangedListeners) {
			listener.vertexPropertyChanged(vertex, key, newValue);
		}
	}

	protected void onEdgePropertyChanged(final Edge edge, final String key, final Object removedValue) {
		for (GraphChangedListener listener : this.graphChangedListeners) {
			listener.edgePropertyChanged(edge, key, removedValue);
		}
	}

	protected void onVertexPropertyRemoved(final Vertex vertex, final String key, final Object newValue) {
		for (GraphChangedListener listener : this.graphChangedListeners) {
			listener.vertexPropertyRemoved(vertex, key, newValue);
		}
	}

	protected void onEdgePropertyRemoved(final Edge edge, final String key, final Object removedValue) {
		for (GraphChangedListener listener : this.graphChangedListeners) {
			listener.edgePropertyRemoved(edge, key, removedValue);
		}
	}

	@Override
	public Set<String> getPropertyKeys() {
		return this.rawElement.getPropertyKeys();
	}

	@Override
	public Object getId() {
		return this.rawElement.getId();
	}

	/**
	 * Raises a vertexPropertyRemoved or edgePropertyRemoved event.
	 */
	@Override
	public Object removeProperty(final String key) {
		Object propertyRemoved = this.rawElement.removeProperty(key);

		if (this instanceof Vertex) {
			this.onVertexPropertyRemoved((Vertex) this, key, propertyRemoved);
		} else if (this instanceof Edge) {
			this.onEdgePropertyRemoved((Edge) this, key, propertyRemoved);
		}

		return propertyRemoved;
	}

	@Override
	public Object getProperty(final String key) {
		return this.rawElement.getProperty(key);
	}

	/**
	 * Raises a vertexPropertyRemoved or edgePropertyChanged event.
	 */
	@Override
	public void setProperty(final String key, final Object value) {
		this.rawElement.setProperty(key, value);

		if (this instanceof Vertex) {
			this.onVertexPropertyChanged((Vertex) this, key, value);
		} else if (this instanceof Edge) {
			this.onEdgePropertyChanged((Edge) this, key, value);
		}
	}

	@Override
	public String toString() {
		return this.rawElement.toString();
	}

	@Override
	public int hashCode() {
		return this.rawElement.hashCode();
	}

	@Override
	public boolean equals(final Object object) {
		return null != object && (object.getClass().equals(this.getClass()))
				&& this.rawElement.getId().equals(((EventElement) object).getId());
	}

	public Element getRawElement() {
		return this.rawElement;
	}

	@Override
	public boolean hasId(Object identifier) {
		return this.rawElement.hasId(identifier);
	}
}
