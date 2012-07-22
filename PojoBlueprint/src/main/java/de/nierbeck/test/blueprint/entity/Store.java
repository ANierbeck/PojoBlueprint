package de.nierbeck.test.blueprint.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Store {
	
	@Id
	private int id;

	private Integer state;

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}
}
