package de.nierbeck.test.blueprint.jpa;

import de.nierbeck.test.blueprint.entity.Store;


public interface MyService {

	Store load(long id);
	
	Store safe(Store object);
	
}