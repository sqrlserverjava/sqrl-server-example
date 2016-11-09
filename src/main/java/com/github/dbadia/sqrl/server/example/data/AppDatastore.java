package com.github.dbadia.sqrl.server.example.data;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.github.dbadia.sqrl.server.example.Constants;
import com.github.dbadia.sqrl.server.exception.SqrlPersistenceException;

/**
 * The datastore for the example app. This is unchanged vs pre-SQRL
 *
 * @author Dave Badia
 *
 */
public class AppDatastore {
	private static final AppDatastore	INSTANCE				= new AppDatastore();
	private static EntityManagerFactory	entityManagerFactory	= Persistence
			.createEntityManagerFactory(Constants.APP_PERSISTENCE_UNIT_NAME);

	public static AppDatastore getInstance() {
		return INSTANCE;
	}

	public AppUser fetchUserByUsername(final String username) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			final List<AppUser> resultList = entityManager
					.createQuery("SELECT u FROM AppUser u WHERE u.username = :aUsername")
					.setParameter("aUsername", username).getResultList();
			if (resultList == null || resultList.isEmpty()) {
				return null;
			} else if (resultList.size() > 1) {
				throw new SqrlPersistenceException(
						"App user result list contained multiple entries for username: " + username);
			} else {
				return resultList.get(0);
			}
		} finally {
			entityManager.close();
		}
	}

	public AppUser fetchUserById(final long id) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			return entityManager.find(AppUser.class, id);
		} finally {
			entityManager.close();
		}
	}

	public void createUser(final AppUser user) {
		if (fetchUserByUsername(user.getUsername()) != null) {
			throw new SqrlPersistenceException("AppUser already exists for username " + user.getUsername());
		}
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.persist(user);
			entityManager.getTransaction().commit();
			if (user.getId() == 0) {
				throw new SqrlPersistenceException("AppUser ID should not be zero after persist");
			}
		} finally {
			entityManager.close();
		}
	}

	public void updateUser(final AppUser user) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.merge(user);
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
	}

	public static void main(final String[] args) {
		Persistence.generateSchema("exampleapp-persistence", Collections.emptyMap());
	}
}
