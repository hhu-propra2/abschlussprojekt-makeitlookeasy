package de.propra2.ausleiherino24.model;

import java.util.Arrays;
import java.util.List;

public enum Category {
	AUTO("Auto"),
	RAD("Rad"),
	BOOT("Boot"),
	IMMOBILIEN("Immobilien"),
	ELEKTRONIK("Elektronik"),
	BÜCHER("Bücher"),
	FILME("Filme"),
	MUSIK("Musik"),
	WERKZEUG("Werkzeug"),
	SPIELZEUG("Spielzeug");

	private String name;

	Category(String name){
		this.name = name;
	}

	public String getName(){
		return name;
	}

	/**
	 * @return list with all existing categories.
	 */
	public List<Category> getAllCategories(){
		return Arrays.asList(values());
	}
}
