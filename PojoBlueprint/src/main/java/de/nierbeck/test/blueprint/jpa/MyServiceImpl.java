package de.nierbeck.test.blueprint.jpa;

import javax.persistence.EntityManager;

import de.nierbeck.test.blueprint.entity.Store;

public class MyServiceImpl implements MyService {

	private EntityManager entityManager;
	
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public MyServiceImpl() {
		super();
	}
	
	@Override
	public Store load(long id) {
		return entityManager.find(Store.class, id);
	}

	@Override
	public Store safe(Store store) {
		return entityManager.merge(store);
	}

}