package com.github.dbadia.sqrl.server.example.data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * The table where the example app stores userdata. Note this table has nothing SQRL specific in it
 * 
 * @author Dave Badia
 *
 */
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@TableGenerator(name = "app_user_gen", table = "app_db_id_gen", pkColumnName = "name", valueColumnName = "value",
			allocationSize = 1)
	@GeneratedValue(generator = "app_user_gen")
	@Column(name = "id")
	private long id;

	@Column(nullable = true)
	private String username;

	@Column(nullable = false)
	private String givenName;

	@Column(nullable = false)
	private String welcomePhrase;

	@Column(nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdAt;

	public AppUser() {
		// Required by JPA
	}

	public AppUser(final String username) {
		this.username = username;
		this.createdAt = new Date();
	}

	public AppUser(final String givenName, final String welcomePhrase) {
		this.givenName = givenName;
		this.welcomePhrase = welcomePhrase;
		this.createdAt = new Date();
	}

	public long getId() {
		return id;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getWelcomePhrase() {
		return welcomePhrase;
	}

	public void setGivenName(final String givenName) {
		this.givenName = givenName;
	}

	public void setWelcomePhrase(final String welcomePhrase) {
		this.welcomePhrase = welcomePhrase;
	}

	public String getUsername() {
		return username;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(final Date createdAt) {
		this.createdAt = createdAt;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public void setUsername(final String username) {
		this.username = username;
	}
}
